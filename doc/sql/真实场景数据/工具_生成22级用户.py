"""
工具_生成22级用户.py — 自动生成 22 级 教师/学生/任课关系 三套 SQL

[v2 升级 — 真实教研室分工]
输出:
    16_22级教师数据.sql      ~115 位教师 (15 公共教研室 + 100 专业课, 每专业 4 位)
                            + teacher_class 关联 (仅专业课教师挂班级)
    17_22级学生数据.sql      1089 位学生 (33 班 × 33 人 - 现有 2 个)
    18_22级教师任课关系.sql  ~700 条 teacher_subject (按课程类别精准分配)

任课规则:
    1. 通识公共课 (思政/外语/数学/物理/心理...) → 对应教研室教师池 round-robin
       - 高数A/英语/日语/线代/概率 复用 24 级公共教师 (王建华/徐玲花等)
       - 思政/军事/心理/物理/语文/信息素养 新增公共教研室教师
    2. 专业核心+选修 → 该专业 4 位专业课教师 round-robin
       - 每位专业课教师只教本专业课, 不跨教研室

依赖: Python 3.9+ + pymysql
"""
import random
from pathlib import Path
import pymysql

random.seed(2022)  # 可复现

# pymysql 连接参数
DB_CONFIG = dict(
    host='127.0.0.1', port=3306,
    user='root', password='12345678',
    database='online_exam_system',
    charset='utf8mb4',
)

# ============================================================================
# 1. 配置区
# ============================================================================
OUTPUT_DIR = Path(__file__).parent
TEACHER_SQL = OUTPUT_DIR / '16_22级教师数据.sql'
STUDENT_SQL = OUTPUT_DIR / '17_22级学生数据.sql'
TS_SQL = OUTPUT_DIR / '18_22级教师任课关系.sql'

# id 段位 (避开现有 100-690 教师 + 200-694 学生)
TEACHER_ID_START = 700
STUDENT_ID_START = 1000

# 密码 = 123456 的 BCrypt 哈希 (复用现有 sys_user.id=694)
PASSWORD_BCRYPT = '$2a$10$6d9o0ZL1fKx/Zr9AR9CzjuhvPqGFZXPUql/xlzBsmMlaA.4xqRb6u'

STUDENTS_PER_CLASS = 33  # 每班 33 人 (计科 2 班共 66 ≈ 用户记忆的 67-68)
TEACHERS_PER_MAJOR = 4   # 每专业 4 位专业课教师 (只教本专业核心+选修, 不跨教研室)

# ============================================================================
# 1.5 课程类别识别 + 公共教研室分配规则
# ============================================================================

# 课程类别识别规则 (按 subject_name 关键字, 优先级从上到下)
COURSE_CATEGORY_RULES = [
    ('sizheng', ['思想道德', '中国近现代史', '马克思', '毛泽东', '形势与政策']),
    ('junshi', ['军事']),
    ('xinli_jiuye', ['心理健康', '职业发展', '创新创业', '大学生职业']),
    ('gongshu_A', ['高等数学A']),
    ('gongshu_B', ['高等数学B']),
    ('gongshu_C', ['高等数学C']),
    ('linear_algebra', ['线性代数']),
    ('gailv', ['概率论']),
    ('daxue_yingyu', ['大学英语']),
    ('jichu_riyu_gong', ['基础日语', '日语听力']),  # 公共日语 (仅日语专业)
    ('daxue_wuli', ['大学物理']),
    ('daxue_yuwen', ['大学语文']),
    ('xinxi_suyang', ['信息检索', '计算思维']),  # 不含编程类
]


def categorize_course(name: str) -> str:
    """识别课程类别. 默认返回 'zhuanye' (专业课)"""
    for cat, keywords in COURSE_CATEGORY_RULES:
        if any(k in name for k in keywords):
            return cat
    return 'zhuanye'


# 24 级现有教师 id (按类别可复用为 22 级公共课教师)
PUBLIC_TEACHER_REUSE = {
    'gongshu_A': [100, 104],          # 王建华, 李春梅 (24 级高数A 老师)
    'daxue_yingyu': [107, 119, 122],  # 徐玲花, 王秀兰, 孙东辰
    'jichu_riyu_gong': [106, 120],    # 何婷婷, 马晓燕
    'linear_algebra': [123],          # 罗敏峰
    'gailv': [123],                   # 罗敏峰
}

# 新增公共教研室教师规格 (id 从 700 起, 按教研室连续编号)
# (real_name, username, category)
NEW_PUBLIC_TEACHERS_SPEC = [
    # 思政教研室 (5 位): 思想道德 / 近现代史 / 马克思 / 毛泽东 / 形势与政策
    ('张文革', 'zhangwenge', 'sizheng'),
    ('李红', 'lihong2022', 'sizheng'),
    ('陈建国', 'chenjianguo', 'sizheng'),
    ('刘忠诚', 'liuzhongcheng', 'sizheng'),
    ('王立军', 'wanglijun', 'sizheng'),
    # 军事理论 (1 位)
    ('赵卫国', 'zhaoweiguo', 'junshi'),
    # 心理就业 (2 位): 心理健康 / 职业发展 / 创新创业
    ('黄欣怡', 'huangxinyi2022', 'xinli_jiuye'),
    ('陈雅静', 'chenyajing', 'xinli_jiuye'),
    # 高数 B (2 位)
    ('张铭杰', 'zhangmingjie', 'gongshu_B'),
    ('刘文博', 'liuwenbo', 'gongshu_B'),
    # 高数 C (1 位)
    ('钱思源', 'qiansiyuan', 'gongshu_C'),
    # 大学物理 (2 位)
    ('林学英', 'linxueying', 'daxue_wuli'),
    ('吴海涛', 'wuhaitao', 'daxue_wuli'),
    # 大学语文 (1 位)
    ('郑文清', 'zhengwenqing', 'daxue_yuwen'),
    # 信息素养 (1 位)
    ('沈雅琴', 'shenyaqin', 'xinxi_suyang'),
]  # 共 15 位

# 跨专业教师复用 (125/126/127 是 22 级未开班的新设专业, 课程由相近专业教师代教)
MAJOR_CROSS_REUSE = {
    125: 100,  # 数据科学与大数据技术 → 计算机科学与技术 (同智制院 + 同领域)
    126: 121,  # 跨境电商 → 物流管理 (同管理学院 + 同领域)
    127: 103,  # 增材制造 → 机械设计制造及其自动化 (同智制院 + 同方向)
}

# ============================================================================
# 2. 5 位学号代码对照表 (edu_major.id -> 5 位代码)
#    规则: <招生代码 2 位><107 + (major.id - 100)> 内部代码
#    例: 计科 = 30 + 107 (= 30107, 兼容样本); 通信 = 32 + 108 = 32108
# ============================================================================
MAJOR_CODE_MAP = {
    100: '30107',  # 计算机科学与技术  (招生 30, 兼容样本)
    101: '32108',  # 通信工程         (招生 32)
    102: '29109',  # 电子信息工程     (招生 29)
    103: '33110',  # 机械设计制造及其自动化 (招生 33)
    104: '31111',  # 电气工程及其自动化  (招生 31)
    105: '28112',  # 土木工程         (招生 28)
    106: '10113',  # 日语             (招生 10)
    107: '09114',  # 英语             (招生 09)
    108: '07115',  # 汉语言文学       (招生 07)
    109: '08116',  # 秘书学           (招生 08)
    110: '24117',  # 环境工程         (招生 24)
    111: '26118',  # 给排水科学与工程  (招生 26)
    112: '25119',  # 建筑环境与能源应用工程 (招生 25)
    113: '01120',  # 风景园林         (招生 01)
    114: '11121',  # 人文地理与城乡规划 (招生 11)
    115: '13122',  # 工程管理         (招生 13)
    116: '14123',  # 工程造价         (招生 14, 推测)
    117: '02124',  # 市场营销         (招生 02)
    118: '05125',  # 财务管理         (招生 05)
    119: '06126',  # 人力资源管理     (招生 06)
    120: '04127',  # 酒店管理         (招生 04)
    121: '03128',  # 物流管理         (招生 03)
    122: '38129',  # 视觉传达设计     (艺术院, 虚构)
    123: '39130',  # 环境设计         (艺术院, 虚构)
    124: '40131',  # 音乐学           (艺术院, 虚构)
}

# ============================================================================
# 3. 班级清单 (按 major_id 排序, 跳过 125/126/127 新专业 22 级未开班)
#    (class_id, class_name, major_id, 班序号 1 或 2)
# ============================================================================
CLASSES = [
    (200, '计算机2201', 100, 1),
    (201, '计算机2202', 100, 2),
    (216, '通信工程2201', 101, 1),
    (217, '电子信息2201', 102, 1),
    (218, '机械2201', 103, 1),
    (202, '电气2201', 104, 1),
    (203, '电气2202', 104, 2),
    (204, '土木2201', 105, 1),
    (205, '土木2202', 105, 2),
    (219, '日语2201', 106, 1),
    (206, '英语2201', 107, 1),
    (207, '英语2202', 107, 2),
    (220, '汉语言2201', 108, 1),
    (221, '秘书学2201', 109, 1),
    (208, '环境工程2201', 110, 1),
    (209, '环境工程2202', 110, 2),
    (210, '给排水2201', 111, 1),
    (211, '给排水2202', 111, 2),
    (222, '建环2201', 112, 1),
    (223, '风景园林2201', 113, 1),
    (224, '人文地理2201', 114, 1),
    (225, '工程管理2201', 115, 1),
    (226, '工程造价2201', 116, 1),
    (227, '市场营销2201', 117, 1),
    (212, '财务管理2201', 118, 1),
    (213, '财务管理2202', 118, 2),
    (228, '人力资源2201', 119, 1),
    (229, '酒店管理2201', 120, 1),
    (214, '物流管理2201', 121, 1),
    (215, '物流管理2202', 121, 2),
    (230, '视觉传达2201', 122, 1),
    (231, '环境设计2201', 123, 1),
    (232, '音乐学2201', 124, 1),
]

# ============================================================================
# 4. 现有数据 (避免冲突)
# ============================================================================
# 现有 22 级学生 (保留, 跳过同号生成)
# key = class_id, value = 按规则该学号应归属的班级内, 需跳过的"班内序号"集合
# 注意学号末三位 = 班序(1位) + 班内序号(2位), 周祥 2230107222 末三位 222 按规则属于
# 班序 2 (即 201 班) 的 22 号, 即使他实际挂 200 班. 因此 201 班生成时要跳过 22.
EXISTING_22_STUDENTS = {
    200: set(),    # 200 班 (班序 1): 无按规则应在此班的已占学号
    201: {22, 3},  # 201 班 (班序 2): 跳 22 (周祥学号 222) + 跳 3 (陶展学号 203)
}

# 现有 26 位教师 (24 级 + 陆卫忠)
EXISTING_TEACHER_USERNAMES = {
    'wangjianhua', 'sunhaiyan', 'wujunhua', 'hujianjun', 'lichunmei',
    'guoxiumei', 'hetingting', 'xulinghua', 'zhupengfei', 'chenxiaofeng',
    'liufang', 'zhangwenliang', 'yangzhihua', 'maxiaoming', 'zhaomingyu',
    'huangli', 'zhouqiang', 'linhanlin', 'xudonglin', 'wangxiulan',
    'maxiaoyan', 'qianzhiyong', 'sundongchen', 'luominfeng', 'luweizhong'
}
EXISTING_TEACHER_REAL_NAMES = {
    '王建华', '孙海燕', '吴俊华', '胡建军', '李春梅',
    '郭秀梅', '何婷婷', '徐玲花', '朱鹏飞', '陈晓峰',
    '刘芳', '张文亮', '杨志华', '马晓明', '赵明宇',
    '黄丽', '周强', '林涵琳', '徐东林', '王秀兰',
    '马晓燕', '钱志勇', '孙东辰', '罗敏峰', '陆卫忠'
}

# 22 级现有学生姓名 (避免重名)
EXISTING_22_STUDENT_NAMES = {'周祥', '陶展'}

# ============================================================================
# 5. 中文姓名生成器 (含汉字-拼音对照表)
# ============================================================================
SURNAMES = [
    '王', '李', '张', '刘', '陈', '杨', '赵', '黄', '周', '吴',
    '徐', '孙', '胡', '朱', '高', '林', '何', '郭', '马', '罗',
    '梁', '宋', '郑', '谢', '韩', '唐', '冯', '于', '董', '萧',
    '程', '曹', '袁', '邓', '许', '傅', '沈', '曾', '彭', '吕',
    '苏', '卢', '蒋', '蔡', '贾', '丁', '魏', '薛', '叶', '阎',
    '余', '潘', '杜', '戴', '夏', '钟', '汪', '田', '任', '姜',
    '范', '方', '石', '姚', '谭', '廖', '邹', '熊', '金', '陶',
    '郝', '孔', '白', '崔', '康', '毛', '邱', '秦', '江', '史',
]

# 单字名 (男女通用 + 偏男 + 偏女)
GIVEN_M_SINGLE = ['伟', '强', '军', '勇', '杰', '涛', '磊', '超', '峰', '鹏',
                  '波', '辉', '锋', '飞', '宇', '豪', '诚', '凯', '宁', '威',
                  '彬', '斌', '亮', '宏', '俊', '健', '建', '坚']
GIVEN_F_SINGLE = ['静', '丽', '梅', '芳', '艳', '燕', '霞', '红', '娟', '敏',
                  '慧', '雪', '婷', '丹', '洁', '倩', '颖', '萍', '娜', '璐',
                  '玲', '蓉', '欣', '雯', '瑶', '青', '思', '清']

# 双字名首字 + 尾字
GIVEN_FIRST = ['志', '建', '文', '俊', '海', '伟', '明', '国', '春', '永',
               '宏', '云', '宇', '雪', '丽', '美', '艳', '秀', '芳', '玲',
               '晓', '亚', '子', '若', '思', '雅', '梦', '佳', '辰', '欣',
               '梓', '雨', '紫', '茜', '希', '诗', '若', '语', '诗']
GIVEN_SECOND = ['强', '华', '军', '杰', '峰', '伟', '明', '俊', '辉', '波',
                '红', '梅', '丽', '英', '燕', '芳', '娟', '婷', '欣', '颖',
                '诚', '远', '航', '博', '睿', '辰', '宇', '昊', '泽', '怡',
                '萱', '彤', '妍', '琪', '琳', '蕾', '蕊', '婧', '雯']

# 汉字 -> 拼音
PINYIN = {
    # 姓
    '王': 'wang', '李': 'li', '张': 'zhang', '刘': 'liu', '陈': 'chen',
    '杨': 'yang', '赵': 'zhao', '黄': 'huang', '周': 'zhou', '吴': 'wu',
    '徐': 'xu', '孙': 'sun', '胡': 'hu', '朱': 'zhu', '高': 'gao',
    '林': 'lin', '何': 'he', '郭': 'guo', '马': 'ma', '罗': 'luo',
    '梁': 'liang', '宋': 'song', '郑': 'zheng', '谢': 'xie', '韩': 'han',
    '唐': 'tang', '冯': 'feng', '于': 'yu', '董': 'dong', '萧': 'xiao',
    '程': 'cheng', '曹': 'cao', '袁': 'yuan', '邓': 'deng', '许': 'xu',
    '傅': 'fu', '沈': 'shen', '曾': 'zeng', '彭': 'peng', '吕': 'lv',
    '苏': 'su', '卢': 'lu', '蒋': 'jiang', '蔡': 'cai', '贾': 'jia',
    '丁': 'ding', '魏': 'wei', '薛': 'xue', '叶': 'ye', '阎': 'yan',
    '余': 'yu', '潘': 'pan', '杜': 'du', '戴': 'dai', '夏': 'xia',
    '钟': 'zhong', '汪': 'wang', '田': 'tian', '任': 'ren', '姜': 'jiang',
    '范': 'fan', '方': 'fang', '石': 'shi', '姚': 'yao', '谭': 'tan',
    '廖': 'liao', '邹': 'zou', '熊': 'xiong', '金': 'jin', '陶': 'tao',
    '郝': 'hao', '孔': 'kong', '白': 'bai', '崔': 'cui', '康': 'kang',
    '毛': 'mao', '邱': 'qiu', '秦': 'qin', '江': 'jiang', '史': 'shi',
    # 名字字符
    '伟': 'wei', '强': 'qiang', '军': 'jun', '勇': 'yong', '杰': 'jie',
    '涛': 'tao', '磊': 'lei', '超': 'chao', '峰': 'feng', '鹏': 'peng',
    '波': 'bo', '辉': 'hui', '锋': 'feng', '飞': 'fei', '宇': 'yu',
    '豪': 'hao', '诚': 'cheng', '凯': 'kai', '宁': 'ning', '威': 'wei',
    '彬': 'bin', '斌': 'bin', '亮': 'liang', '宏': 'hong', '俊': 'jun',
    '健': 'jian', '建': 'jian', '坚': 'jian',
    '静': 'jing', '丽': 'li', '梅': 'mei', '芳': 'fang', '艳': 'yan',
    '燕': 'yan', '霞': 'xia', '红': 'hong', '娟': 'juan', '敏': 'min',
    '慧': 'hui', '雪': 'xue', '婷': 'ting', '丹': 'dan', '洁': 'jie',
    '倩': 'qian', '颖': 'ying', '萍': 'ping', '娜': 'na', '璐': 'lu',
    '玲': 'ling', '蓉': 'rong', '欣': 'xin', '雯': 'wen', '瑶': 'yao',
    '青': 'qing', '思': 'si', '清': 'qing',
    '志': 'zhi', '文': 'wen', '海': 'hai', '明': 'ming', '国': 'guo',
    '春': 'chun', '永': 'yong', '云': 'yun', '美': 'mei', '秀': 'xiu',
    '晓': 'xiao', '亚': 'ya', '子': 'zi', '若': 'ruo', '雅': 'ya',
    '梦': 'meng', '佳': 'jia', '辰': 'chen',
    '梓': 'zi', '雨': 'yu', '紫': 'zi', '茜': 'qian', '希': 'xi',
    '诗': 'shi', '语': 'yu',
    '华': 'hua', '英': 'ying',
    '远': 'yuan', '航': 'hang', '博': 'bo', '睿': 'rui', '昊': 'hao',
    '泽': 'ze', '怡': 'yi',
    '萱': 'xuan', '彤': 'tong', '妍': 'yan', '琪': 'qi', '琳': 'lin',
    '蕾': 'lei', '蕊': 'rui', '婧': 'jing',
}


def to_pinyin(name: str) -> str:
    """中文名 -> 全拼 (无声调无空格)"""
    return ''.join(PINYIN.get(c, c) for c in name)


def gen_chinese_name(used_names: set, prefer_gender: str = None) -> str:
    """生成不重复的中文姓名 (最多 100 次重试)"""
    for _ in range(100):
        surname = random.choice(SURNAMES)
        if random.random() < 0.25:  # 25% 单字名
            if prefer_gender == 'm':
                given = random.choice(GIVEN_M_SINGLE)
            elif prefer_gender == 'f':
                given = random.choice(GIVEN_F_SINGLE)
            else:
                given = random.choice(GIVEN_M_SINGLE + GIVEN_F_SINGLE)
        else:  # 75% 双字名
            given = random.choice(GIVEN_FIRST) + random.choice(GIVEN_SECOND)
        name = surname + given
        if name not in used_names:
            used_names.add(name)
            return name
    used_names.add(name)
    return name


def gen_phone() -> str:
    """生成 13 开头的 11 位手机号"""
    return '13' + ''.join(str(random.randint(0, 9)) for _ in range(9))


# ============================================================================
# 6. 三段 SQL 生成
# ============================================================================
SQL_HEADER = """-- ============================================================================
-- 自动生成 (工具_生成22级用户.py) — 请勿手工修改
-- ============================================================================
USE online_exam_system;
SET FOREIGN_KEY_CHECKS = 0;
START TRANSACTION;
"""
SQL_FOOTER = """
COMMIT;
SET FOREIGN_KEY_CHECKS = 1;
"""


def gen_teachers():
    """生成 16 号 SQL: 22 级教师 (15 公共 + 100 专业) + teacher_class 关联

    返回值:
        public_teachers_by_cat: dict[cat -> [teacher_id]] (含 24 级复用 + 新增)
        major_teachers: dict[major_id -> [teacher_id, ...]] 4 位/专业
    """
    used_names = set(EXISTING_TEACHER_REAL_NAMES)
    used_usernames = set(EXISTING_TEACHER_USERNAMES)

    # ---- 1) 公共教研室教师 (id 700-714) ----
    public_teachers = []  # [(id, username, real_name, category), ...]
    next_id = TEACHER_ID_START
    for rname, uname, cat in NEW_PUBLIC_TEACHERS_SPEC:
        assert rname not in used_names, f'公共教师名重复: {rname}'
        assert uname not in used_usernames, f'公共教师 username 重复: {uname}'
        used_names.add(rname)
        used_usernames.add(uname)
        public_teachers.append((next_id, uname, rname, cat))
        next_id += 1

    # ---- 2) 专业课教师 (id 715-814, 25 专业 × 4 位) ----
    major_teachers_data = []  # [(id, username, real_name, major_id), ...]
    for major_id in MAJOR_CODE_MAP.keys():
        for _ in range(TEACHERS_PER_MAJOR):
            name = gen_chinese_name(used_names)
            base = to_pinyin(name)
            username = base
            suffix = 1
            while username in used_usernames:
                suffix += 1
                username = f'{base}{suffix}'
            used_usernames.add(username)
            major_teachers_data.append((next_id, username, name, major_id))
            next_id += 1

    # ---- 3) 生成 SQL ----
    sql = [SQL_HEADER]

    sql.append('\n-- ============================================================')
    sql.append('-- 区段 A: 公共教研室教师 (15 位, id 700-714)')
    sql.append('-- 复用 24 级公共教师: 高数A/英语/日语/线代/概率 不在此新建')
    sql.append('-- ============================================================')
    for tid, uname, rname, cat in public_teachers:
        phone = gen_phone()
        email = f'{uname}@tpxy.usts.edu.cn'
        gender = 1 if random.random() > 0.45 else 2
        sql.append(
            f"INSERT INTO sys_user (id, username, password, real_name, email, phone, gender, role_id, status, create_time) "
            f"VALUES ({tid}, '{uname}', '{PASSWORD_BCRYPT}', '{rname}', '{email}', '{phone}', {gender}, 2, 1, NOW());"
        )

    sql.append('\n-- ============================================================')
    sql.append(f'-- 区段 B: 专业课教师 ({len(major_teachers_data)} 位, id 715-{next_id-1})')
    sql.append(f'-- 25 专业 × {TEACHERS_PER_MAJOR} 位, 仅教本专业核心+选修课')
    sql.append('-- ============================================================')
    for tid, uname, rname, mid in major_teachers_data:
        phone = gen_phone()
        email = f'{uname}@tpxy.usts.edu.cn'
        gender = 1 if random.random() > 0.45 else 2
        sql.append(
            f"INSERT INTO sys_user (id, username, password, real_name, email, phone, gender, role_id, status, create_time) "
            f"VALUES ({tid}, '{uname}', '{PASSWORD_BCRYPT}', '{rname}', '{email}', '{phone}', {gender}, 2, 1, NOW());"
        )

    # ---- 4) teacher_class: 只让专业课教师挂班级 (公共教研室不挂任何班级) ----
    classes_by_major = {}
    for cid, cname, mid, seq in CLASSES:
        classes_by_major.setdefault(mid, []).append(cid)

    sql.append('\n-- ============================================================')
    sql.append('-- teacher_class 关联:')
    sql.append('--   - 公共教研室教师 (15 位): 挂全部 33 个 22 级班级')
    sql.append('--                          (体现"服务全院"的公共教研室定位, 让 grade 筛选可见)')
    sql.append('--   - 专业课教师 (100 位): 挂本专业 1-2 个班级')
    sql.append('-- ============================================================')
    all_22_class_ids = [cid for cid, _, _, _ in CLASSES]
    for tid, uname, rname, cat in public_teachers:
        for cid in all_22_class_ids:
            sql.append(f"INSERT INTO teacher_class (teacher_id, class_id) VALUES ({tid}, {cid});")
    for tid, uname, rname, mid in major_teachers_data:
        for cid in classes_by_major.get(mid, []):
            sql.append(f"INSERT INTO teacher_class (teacher_id, class_id) VALUES ({tid}, {cid});")

    sql.append(SQL_FOOTER)
    sql.append('\n-- ============= 验证 =============')
    sql.append(f"SELECT '22级新增教师数 (期望 {len(public_teachers) + len(major_teachers_data)})' AS metric, COUNT(*) AS actual FROM sys_user WHERE id BETWEEN {TEACHER_ID_START} AND {next_id-1} AND role_id=2;")
    sql.append(
        f"SELECT m.id AS '专业ID', m.major_name AS '专业', COUNT(DISTINCT tc.teacher_id) AS '专业课教师数'\n"
        f"  FROM teacher_class tc JOIN edu_class c ON tc.class_id=c.id JOIN edu_major m ON c.major_id=m.id\n"
        f"  WHERE tc.teacher_id BETWEEN {TEACHER_ID_START} AND {next_id-1}\n"
        f"  GROUP BY m.id ORDER BY m.id;"
    )

    TEACHER_SQL.write_text('\n'.join(sql), encoding='utf-8')
    print(f'[OK] {TEACHER_SQL.name}: {len(public_teachers)} 公共 + {len(major_teachers_data)} 专业 = {len(public_teachers)+len(major_teachers_data)} 位教师')

    # ---- 5) 构建返回值: 公共教师池 (按 category) + 专业教师池 (按 major_id) ----
    public_pool = {}  # cat -> [teacher_id, ...] (新建 + 24 级复用)
    for tid, uname, rname, cat in public_teachers:
        public_pool.setdefault(cat, []).append(tid)
    # 复用 24 级
    for cat, teacher_ids in PUBLIC_TEACHER_REUSE.items():
        public_pool.setdefault(cat, []).extend(teacher_ids)

    major_pool = {}  # major_id -> [teacher_id, ...]
    for tid, uname, rname, mid in major_teachers_data:
        major_pool.setdefault(mid, []).append(tid)

    return public_pool, major_pool


def gen_students():
    """生成 17 号 SQL: 22 级学生 (按 33 班 × 33 人)"""
    used_names = set(EXISTING_22_STUDENT_NAMES)
    students = []  # [(id, username, real_name, class_id), ...]
    next_id = STUDENT_ID_START

    for cid, cname, mid, seq in CLASSES:
        major_code = MAJOR_CODE_MAP[mid]
        existing_seqs = EXISTING_22_STUDENTS.get(cid, set())
        # 该班级要生成的学生序号 (跳过已占用)
        seqs_to_generate = []
        in_class_seq = 1
        while len(seqs_to_generate) < STUDENTS_PER_CLASS:
            if in_class_seq not in existing_seqs:
                seqs_to_generate.append(in_class_seq)
            in_class_seq += 1
            if in_class_seq > 50:  # 防止死循环
                break

        for in_seq in seqs_to_generate:
            # 学号 = 22 + 5位专业代码 + 班序1 + 班内序2
            student_id_str = f'22{major_code}{seq}{in_seq:02d}'
            name = gen_chinese_name(used_names)
            students.append((next_id, student_id_str, name, cid))
            next_id += 1

    sql = [SQL_HEADER]
    sql.append(f'\n-- ---- 22 级学生 INSERT ({len(students)} 位, id {STUDENT_ID_START} 起) ----')
    for sid, uname, rname, cid in students:
        phone = gen_phone()
        email = f'{uname}@stu.tpxy.usts.edu.cn'
        gender = 1 if random.random() > 0.45 else 2
        sql.append(
            f"INSERT INTO sys_user (id, username, password, real_name, email, phone, gender, role_id, class_id, status, create_time) "
            f"VALUES ({sid}, '{uname}', '{PASSWORD_BCRYPT}', '{rname}', '{email}', '{phone}', {gender}, 3, {cid}, 1, NOW());"
        )

    sql.append(SQL_FOOTER)
    sql.append('\n-- ============= 验证 =============')
    sql.append(f"SELECT '22级新增学生数 (期望 {len(students)})' AS metric, COUNT(*) AS actual FROM sys_user WHERE id >= {STUDENT_ID_START} AND role_id=3;")
    sql.append("SELECT c.class_name, COUNT(u.id) AS '学生数' FROM edu_class c LEFT JOIN sys_user u ON u.class_id=c.id AND u.role_id=3 WHERE c.grade='2022级' GROUP BY c.id ORDER BY c.id;")

    STUDENT_SQL.write_text('\n'.join(sql), encoding='utf-8')
    print(f'[OK] {STUDENT_SQL.name}: {len(students)} 位学生')
    return students


def fetch_22_subjects():
    """从 MySQL 查 22 级所有课程, 返回 [(subject_id, subject_name, major_id), ...]"""
    conn = pymysql.connect(**DB_CONFIG)
    try:
        with conn.cursor() as cur:
            cur.execute("SELECT id, subject_name, major_id FROM edu_subject WHERE grade='2022级' ORDER BY major_id, id")
            return list(cur.fetchall())
    finally:
        conn.close()


# 陆卫忠 (id=690) 固定任课的 22 级课程 (尊重历史样本数据)
LUWEIZHONG_TEACHER_ID = 690
LUWEIZHONG_FIXED_SUBJECT_NAMES = ['Java EE 开发技术基础', '面向对象技术']


def fetch_luweizhong_fixed_subjects():
    """查陆卫忠固定任课的 22 级课程 subject_id 列表 (Java EE + 面向对象)"""
    conn = pymysql.connect(**DB_CONFIG)
    try:
        with conn.cursor() as cur:
            placeholders = ','.join(['%s'] * len(LUWEIZHONG_FIXED_SUBJECT_NAMES))
            cur.execute(
                f"SELECT id, subject_name, major_id FROM edu_subject "
                f"WHERE grade='2022级' AND major_id=100 AND subject_name IN ({placeholders})",
                LUWEIZHONG_FIXED_SUBJECT_NAMES
            )
            return list(cur.fetchall())
    finally:
        conn.close()


def gen_teacher_subject(public_pool, major_pool):
    """生成 18 号 SQL: teacher_subject 任课关系 (按课程类别精准分配)

    分配策略:
        - 公共课 (识别为非 'zhuanye') → 对应教研组教师池 round-robin
            公共课包括: 思政/军事/心理就业/高数A/高数B/高数C/线代/概率/英语/日语公共/物理/语文/信息素养
            教研组池 = 24 级复用教师 + 16 号 SQL 新建公共教师
        - 专业课 (cat == 'zhuanye') → 该专业 4 位专业教师 round-robin

    覆盖率: 所有 22 级课程必须有教师 (若公共池为空则跳过并打印警告)
    """
    subjects = fetch_22_subjects()
    print(f'  从数据库查到 22 级课程 {len(subjects)} 门')

    # 陆卫忠 (id=690) 固定 2 门课 (尊重历史样本)
    luwei_subs = fetch_luweizhong_fixed_subjects()
    luwei_reserved_ids = {sid for sid, _, _ in luwei_subs}
    print(f'  陆卫忠 (690) 固定任课: {len(luwei_subs)} 门 ({[n for _, n, _ in luwei_subs]})')

    # ---- 1) 分类统计 + 分配 ----
    assignments = []  # [(teacher_id, subject_id, cat, subject_name, major_id), ...]
    counter_by_cat = {}  # cat -> 出现次数 (用于 round-robin)
    cat_stats = {}       # cat -> count

    unmatched = []  # 公共课无对应教师池

    # 1.a) 先固定 INSERT 陆卫忠的 2 门课
    for sid, sname, mid in luwei_subs:
        assignments.append((LUWEIZHONG_TEACHER_ID, sid, 'zhuanye_luwei', sname, mid))

    # 1.b) round-robin 分配其余课程, 跳过陆卫忠已占
    for sid, sname, mid in subjects:
        cat = categorize_course(sname)
        cat_stats[cat] = cat_stats.get(cat, 0) + 1

        if sid in luwei_reserved_ids:
            continue  # 已分配给陆卫忠, 跳过

        if cat == 'zhuanye':
            pool = major_pool.get(mid, [])
            # 跨专业复用: 125/126/127 未开班专业, 复用相近专业的教师
            if not pool and mid in MAJOR_CROSS_REUSE:
                pool = major_pool.get(MAJOR_CROSS_REUSE[mid], [])
        else:
            pool = public_pool.get(cat, [])

        if not pool:
            unmatched.append((sid, sname, cat, mid))
            continue

        idx = counter_by_cat.get((cat, mid) if cat == 'zhuanye' else cat, 0)
        teacher_id = pool[idx % len(pool)]
        counter_by_cat[(cat, mid) if cat == 'zhuanye' else cat] = idx + 1
        assignments.append((teacher_id, sid, cat, sname, mid))

    # ---- 2) 生成 SQL ----
    sql = [SQL_HEADER]

    sql.append('\n-- ============================================================')
    sql.append('-- teacher_subject 任课关系 (按课程类别精准分配)')
    sql.append('--')
    sql.append('-- 课程类别统计:')
    for cat in sorted(cat_stats.keys()):
        cnt = cat_stats[cat]
        pool = public_pool.get(cat, []) if cat != 'zhuanye' else None
        if cat == 'zhuanye':
            pool_desc = f'专业课 (25 专业 × 4 教师)'
        else:
            pool_desc = f'公共池 {len(pool)} 位'
        sql.append(f'--   {cat:<20} {cnt:>4} 门 → {pool_desc}')
    sql.append('-- ============================================================')

    sql.append('\n-- ---- 公共课任课 ----')
    for tid, sid, cat, sname, mid in assignments:
        if cat == 'zhuanye':
            continue
        sql.append(f"INSERT INTO teacher_subject (teacher_id, subject_id) VALUES ({tid}, {sid}); -- [{cat}] {sname}")

    sql.append('\n-- ---- 专业课任课 ----')
    for tid, sid, cat, sname, mid in assignments:
        if cat != 'zhuanye':
            continue
        sql.append(f"INSERT INTO teacher_subject (teacher_id, subject_id) VALUES ({tid}, {sid}); -- [major={mid}] {sname}")

    if unmatched:
        sql.append('\n-- ---- ⚠ 未分配课程 (公共池为空) ----')
        for sid, sname, cat, mid in unmatched:
            sql.append(f"-- 跳过 subject_id={sid} ({cat}) {sname} [major={mid}]")

    sql.append(SQL_FOOTER)
    sql.append('\n-- ============= 验证 =============')
    sql.append(
        "SELECT '22 级任课关系数' AS metric, COUNT(*) AS actual\n"
        "  FROM teacher_subject ts JOIN edu_subject s ON ts.subject_id=s.id\n"
        "  WHERE s.grade='2022级';"
    )
    sql.append(
        "SELECT u.real_name AS '教师', COUNT(ts.subject_id) AS '任课数'\n"
        "  FROM sys_user u JOIN teacher_subject ts ON ts.teacher_id=u.id\n"
        "  JOIN edu_subject s ON ts.subject_id=s.id\n"
        "  WHERE s.grade='2022级' AND u.id BETWEEN 700 AND 824\n"
        "  GROUP BY u.id ORDER BY COUNT(ts.subject_id) DESC LIMIT 20;"
    )

    TS_SQL.write_text('\n'.join(sql), encoding='utf-8')
    print(f'[OK] {TS_SQL.name}: {len(assignments)} 任课条 (未分配 {len(unmatched)})')
    print(f'     课程类别分布: {dict(sorted(cat_stats.items(), key=lambda x: -x[1]))}')


# ============================================================================
# 7. 主入口
# ============================================================================
import sys

if __name__ == '__main__':
    skip_students = '--skip-students' in sys.argv
    print('=== 生成 22 级教师/学生/任课关系 SQL ===')
    public_pool, major_pool = gen_teachers()
    if not skip_students:
        gen_students()
    else:
        print('[skip] 17 号学生 SQL 已跳过 (数据库已有 1089 学生)')
    gen_teacher_subject(public_pool, major_pool)
    print('\n=== 完成. 执行顺序: 16 → 17 → 18 (建议用 mysql -e "SOURCE TEMP/...") ===')
