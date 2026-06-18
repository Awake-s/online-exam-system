package com.exam.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.exam.common.exception.BusinessException;
import com.exam.common.result.PageResult;
import com.exam.dto.request.MajorAddRequest;
import com.exam.entity.EduClass;
import com.exam.entity.EduMajor;
import com.exam.entity.SubjectMajor;
import com.exam.mapper.EduClassMapper;
import com.exam.mapper.EduMajorMapper;
import com.exam.mapper.SubjectMajorMapper;
import com.exam.service.MajorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class MajorServiceImpl implements MajorService {

    @Autowired private EduMajorMapper majorMapper;
    @Autowired private EduClassMapper classMapper;
    @Autowired private SubjectMajorMapper subjectMajorMapper;

    @Override
    public PageResult<Map<String, Object>> listMajors(Integer page, Integer size, String majorName) {
        LambdaQueryWrapper<EduMajor> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(majorName)) wrapper.like(EduMajor::getMajorName, majorName);
        wrapper.orderByDesc(EduMajor::getCreateTime);

        Page<EduMajor> p = majorMapper.selectPage(new Page<>(page, size), wrapper);
        List<EduMajor> majors = p.getRecords();
        if (majors.isEmpty()) {
            return new PageResult<>(Collections.emptyList(), 0L, p.getCurrent(), p.getSize());
        }

        // 批量查询每个专业的班级数（按 major_id 分组计数）
        List<Long> majorIds = majors.stream().map(EduMajor::getId).collect(Collectors.toList());
        Map<Long, Long> classCountMap = new HashMap<>();
        classMapper.selectList(new LambdaQueryWrapper<EduClass>()
                        .in(EduClass::getMajorId, majorIds).select(EduClass::getMajorId))
                .forEach(c -> classCountMap.merge(c.getMajorId(), 1L, Long::sum));

        // 批量查询每个专业的科目数
        Map<Long, Long> subjectCountMap = new HashMap<>();
        subjectMajorMapper.selectList(new LambdaQueryWrapper<SubjectMajor>()
                        .in(SubjectMajor::getMajorId, majorIds).select(SubjectMajor::getMajorId))
                .forEach(sm -> subjectCountMap.merge(sm.getMajorId(), 1L, Long::sum));

        List<Map<String, Object>> records = majors.stream().map(m -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", m.getId());
            map.put("majorName", m.getMajorName());
            map.put("description", m.getDescription());
            map.put("classCount", classCountMap.getOrDefault(m.getId(), 0L));
            map.put("subjectCount", subjectCountMap.getOrDefault(m.getId(), 0L));
            map.put("createTime", m.getCreateTime());
            return map;
        }).collect(Collectors.toList());
        return new PageResult<>(records, p.getTotal(), p.getCurrent(), p.getSize());
    }

    @Override
    public void addMajor(MajorAddRequest request) {
        long count = majorMapper.selectCount(new LambdaQueryWrapper<EduMajor>().eq(EduMajor::getMajorName, request.getMajorName()));
        if (count > 0) throw new BusinessException("专业名称已存在");
        EduMajor major = new EduMajor();
        major.setMajorName(request.getMajorName());
        major.setDescription(request.getDescription());
        majorMapper.insert(major);
    }

    @Override
    public void updateMajor(Long id, MajorAddRequest request) {
        EduMajor major = majorMapper.selectById(id);
        if (major == null) throw new BusinessException("专业不存在");
        long count = majorMapper.selectCount(new LambdaQueryWrapper<EduMajor>()
                .eq(EduMajor::getMajorName, request.getMajorName()).ne(EduMajor::getId, id));
        if (count > 0) throw new BusinessException("专业名称已存在");
        major.setMajorName(request.getMajorName());
        major.setDescription(request.getDescription());
        majorMapper.updateById(major);
    }

    @Override
    public void deleteMajor(Long id) {
        long classCount = classMapper.selectCount(new LambdaQueryWrapper<EduClass>().eq(EduClass::getMajorId, id));
        if (classCount > 0) throw new BusinessException("该专业下有班级，无法删除");
        long subjectCount = subjectMajorMapper.selectCount(new LambdaQueryWrapper<SubjectMajor>().eq(SubjectMajor::getMajorId, id));
        if (subjectCount > 0) throw new BusinessException("该专业下有科目，无法删除");
        majorMapper.deleteById(id);
    }

    @Override
    public List<Map<String, Object>> getAllMajors() {
        return majorMapper.selectList(new LambdaQueryWrapper<EduMajor>().orderByAsc(EduMajor::getMajorName))
                .stream().map(m -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", m.getId());
                    map.put("majorName", m.getMajorName());
                    return map;
                }).collect(Collectors.toList());
    }
}
