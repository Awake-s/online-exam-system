/**
 * 「按需维度消歧」科目下拉显示算法
 *
 * 设计动机：教师任课情况差异巨大 ——
 *   - 专业课老师（如只教计算机的 luweizhong）：每条科目唯一，加任何后缀都是冗余噪音
 *   - 公共课老师（如教多专业思政课的 zhangwenge）：同名科目仅专业不同，无需年级
 *   - 跨年级老师（教 22+23 级同一专业课）：同名科目仅年级不同，无需专业
 *   - 跨年级跨专业老师：同名集合年级专业都不同，年级专业都需要
 *
 * 任何静态格式都不可能同时满足所有教师，必须按维度差异性动态消歧。
 *
 * 算法（O(N) 一次预处理 + O(1) 查找）：
 *   对每个 subject s：
 *     1) G = {x ∈ subjects | x.subjectName == s.subjectName}   — 同名兄弟集合
 *     2) |G| == 1            → 唯一，无需后缀
 *     3) gradeSet = G.map(grade).deduped
 *        majorSet = G.map(majorName).deduped
 *        后缀 parts = []
 *        if |gradeSet| > 1 && s.grade   → parts.push(s.grade)
 *        if |majorSet| > 1 && s.majorName → parts.push(s.majorName)
 *        |parts| == 0 ? subjectName : `${subjectName}（${parts.join(' · ')}）`
 *
 * 边界兜底：
 *   - subjects 为空 / 非数组    → 返回空 Map
 *   - subject 缺 id            → 跳过该项
 *   - subject 缺 subjectName   → 在 Map 中映射为空字符串
 *   - 维度字段为 null/undefined → 该维度算空字符串参与去重；后缀不显示该维度
 *   - 同名集合所有维度都无差异（理论极端：完全数据重复）→ 不加后缀，避免空括号
 */

export interface SubjectLike {
  id: number | string
  subjectName?: string | null
  grade?: string | null
  majorName?: string | null
}

/**
 * 一次性预处理科目列表，生成 id → 显示 label 的映射。
 * 推荐配合 Vue computed 使用：subjects 不变时自动缓存。
 */
export function buildSubjectLabelMap(
  subjects: SubjectLike[] | null | undefined
): Map<number | string, string> {
  const result = new Map<number | string, string>()
  if (!Array.isArray(subjects) || subjects.length === 0) return result

  // Phase 1: 按 subjectName 分组，形成同名兄弟集合
  const byName = new Map<string, SubjectLike[]>()
  for (const s of subjects) {
    if (!s || s.id == null || !s.subjectName) continue
    const list = byName.get(s.subjectName)
    if (list) list.push(s)
    else byName.set(s.subjectName, [s])
  }

  // Phase 2: 按维度差异性动态生成 label
  for (const s of subjects) {
    if (!s || s.id == null) continue
    if (!s.subjectName) {
      result.set(s.id, '')
      continue
    }
    const siblings = byName.get(s.subjectName) || []
    if (siblings.length <= 1) {
      result.set(s.id, s.subjectName)
      continue
    }
    // 同名兄弟存在 → 检测维度差异
    const gradeSet = new Set(siblings.map((x) => x.grade || ''))
    const majorSet = new Set(siblings.map((x) => x.majorName || ''))
    const parts: string[] = []
    if (gradeSet.size > 1 && s.grade) parts.push(s.grade)
    if (majorSet.size > 1 && s.majorName) parts.push(s.majorName)
    result.set(
      s.id,
      parts.length > 0 ? `${s.subjectName}（${parts.join(' · ')}）` : s.subjectName
    )
  }

  return result
}

/**
 * 单条 label 查找。labelMap 缺失或 subjectId 为空时回退到 fallbackName。
 */
export function getSubjectLabel(
  subjectId: number | string | null | undefined,
  labelMap: Map<number | string, string>,
  fallbackName?: string | null
): string {
  if (subjectId == null) return fallbackName || ''
  const label = labelMap.get(subjectId)
  if (label !== undefined && label !== '') return label
  return fallbackName || ''
}
