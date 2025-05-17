package com.sky.service.impl;

import com.sky.dto.GoodsSalesDTO;
import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.ReportService;
import com.sky.vo.OrderReportVO;
import com.sky.vo.SalesTop10ReportVO;
import com.sky.vo.TurnoverReportVO;
import com.sky.vo.UserReportVO;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.util.StringUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ReportServiceImpl implements ReportService {
    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private UserMapper userMapper;

    @Override
    public TurnoverReportVO getTurnoverStatistics(LocalDate begin,LocalDate end) {
        List<LocalDate> dateList=new ArrayList<>();
        dateList.add(begin);
        while (!begin.equals(end)) {
            begin = begin.plusDays(1);
            dateList.add(begin);
        }

        List<Double> turnoverList=new ArrayList<>();
        for(LocalDate date:dateList){
            LocalDateTime beginTime=LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime=LocalDateTime.of(date, LocalTime.MAX);

            Map map=new HashMap();
            map.put("beginTime",beginTime);
            map.put("endTime",endTime);
            map.put("status", Orders.COMPLETED);
            Double turnover=orderMapper.sumByMap(map);
            turnover=turnover==null?0.0:turnover;
            turnoverList.add(turnover);
        }


        return TurnoverReportVO
                .builder()
                .dateList(StringUtils.join(dateList, ","))
                .turnoverList(StringUtils.join(turnoverList, ","))
                .build();
    }

    @Override
    public UserReportVO getUserStatistics(LocalDate begin, LocalDate end) {
        List<LocalDate> dateList=new ArrayList<>();
        dateList.add(begin);
        while (!begin.equals(end)) {
            begin = begin.plusDays(1);
            dateList.add(begin);
        }

        List<Integer> totalUserList=new ArrayList<>();
        List<Integer> newUserList=new ArrayList<>();
        for(LocalDate date:dateList){
            LocalDateTime beginTime=LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime=LocalDateTime.of(date, LocalTime.MAX);

            Map map=new HashMap();
            map.put("endTime",endTime);
            Integer totalUser=userMapper.countByMap(map);
            totalUserList.add(totalUser);

            map.put("beginTime",beginTime);
            Integer newUser=userMapper.countByMap(map);
            newUserList.add(newUser);


        }
        return UserReportVO.builder()
                .dateList(StringUtils.join(dateList, ","))
                .totalUserList(StringUtils.join(totalUserList, ","))
                .newUserList(StringUtils.join(newUserList, ","))
                .build();
    }

    @Override
    public OrderReportVO getOrderStatistics(LocalDate begin, LocalDate end) {
        List<LocalDate> dateList=new ArrayList<>();
        dateList.add(begin);
        while (!begin.equals(end)) {
            begin = begin.plusDays(1);
            dateList.add(begin);
        }

        List<Integer> totalOrderList=new ArrayList<>();
        List<Integer> validOrderList=new ArrayList<>();
        for(LocalDate date:dateList){
            LocalDateTime beginTime=LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime=LocalDateTime.of(date, LocalTime.MAX);

            Map map=new HashMap();
            map.put("endTime",endTime);
            map.put("beginTime",beginTime);

            Integer totalOrder=orderMapper.countByMap(map);
            totalOrderList.add(totalOrder);

            map.put("status",Orders.COMPLETED);
            Integer validOrder=orderMapper.countByMap(map);
            validOrderList.add(validOrder);


        }
        return OrderReportVO.builder()
                .dateList(StringUtils.join(dateList, ","))
                .orderCompletionRate((double) (validOrderList.size()/totalOrderList.size()))
                .orderCountList(StringUtils.join(totalOrderList, ","))
                .totalOrderCount(totalOrderList.stream().reduce(Integer::sum).get())
                .validOrderCount(validOrderList.stream().reduce(Integer::sum).get())
                .validOrderCountList(StringUtils.join(validOrderList, ","))
                .build();
    }

    @Override
    public SalesTop10ReportVO getSalesTop10Statistics(LocalDate begin, LocalDate end) {
        LocalDateTime beginTime=LocalDateTime.of(begin, LocalTime.MIN);
        LocalDateTime endTime=LocalDateTime.of(end, LocalTime.MAX);
        List<GoodsSalesDTO>goodsSalesDTOList=orderMapper.getSalesTop(beginTime,endTime);

        List<String>nameList= goodsSalesDTOList.stream().map(GoodsSalesDTO::getName).collect(Collectors.toList());
        List<Integer>numbers= goodsSalesDTOList.stream().map(GoodsSalesDTO::getNumber).collect(Collectors.toList());



        return SalesTop10ReportVO.builder()
                .nameList(StringUtils.join(nameList, ","))
                .numberList(StringUtils.join(numbers, ","))
                .build();
    }
}
