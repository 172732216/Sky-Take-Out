package com.sky.service.impl;

import com.sky.dto.GoodsSalesDTO;
import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.ReportService;
import com.sky.service.WorkspaceService;
import com.sky.vo.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.util.StringUtil;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
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
    @Autowired
    private WorkspaceService workspaceService;

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

    @Override
    public void exportBusinessData(HttpServletResponse response) throws IOException {
        LocalDate dateBegin=LocalDate.now().minusDays(30);
        LocalDate dateEnd=LocalDate.now().minusDays(1);

        BusinessDataVO businessDataVO= workspaceService.getBusinessData(LocalDateTime.of(dateBegin,LocalTime.MIN),LocalDateTime.of(dateEnd,LocalTime.MAX));

        InputStream in= this.getClass().getClassLoader().getResourceAsStream("template/运营数据报表模板.xlsx");
        XSSFWorkbook excel=new XSSFWorkbook(in);
        XSSFSheet sheet=excel.getSheetAt(0);
        sheet.getRow(1).getCell(1).setCellValue("时间："+dateBegin+"至"+dateEnd);
        sheet.getRow(3).getCell(2).setCellValue(businessDataVO.getTurnover());
        sheet.getRow(3).getCell(4).setCellValue(businessDataVO.getOrderCompletionRate());
        sheet.getRow(3).getCell(6).setCellValue(businessDataVO.getNewUsers());

        sheet.getRow(5).getCell(3).setCellValue(businessDataVO.getValidOrderCount());
        sheet.getRow(5).getCell(5).setCellValue(businessDataVO.getUnitPrice());
        //明细数据
        for(int i=0;i<30;i++){
            LocalDate date=dateBegin.plusDays(i);
            businessDataVO= workspaceService.getBusinessData(LocalDateTime.of(date,LocalTime.MIN),LocalDateTime.of(date,LocalTime.MAX));
            sheet.getRow(7+i).getCell(1).setCellValue(String.valueOf(date));
            sheet.getRow(7+i).getCell(2).setCellValue(businessDataVO.getTurnover());
            sheet.getRow(7+i).getCell(3).setCellValue(businessDataVO.getValidOrderCount());
            sheet.getRow(7+i).getCell(4).setCellValue(businessDataVO.getOrderCompletionRate());
            sheet.getRow(7+i).getCell(5).setCellValue(businessDataVO.getUnitPrice());
            sheet.getRow(7+i).getCell(6).setCellValue(businessDataVO.getNewUsers());
        }


        ServletOutputStream out=response.getOutputStream();
        excel.write(out);
        out.close();
        excel.close();
    }
}
