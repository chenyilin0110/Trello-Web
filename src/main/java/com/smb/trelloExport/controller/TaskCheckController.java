package com.smb.trelloExport.controller;

import com.smb.trelloExport.bean.*;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.JSON;
import com.smb.trelloExport.service.TrelloExportService;
import com.smb.trelloExport.utility.ConfigLoader;
import com.smb.trelloExport.utility.Utility;
import com.smb.trelloExport.utility.WebAPI;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;

@Controller
@RequestMapping("/task")
public class TaskCheckController {

    private static Logger logger = LoggerFactory.getLogger(TaskCheckController.class);

    @Autowired
    private TrelloExportService trelloExportService;

    @RequestMapping(value = "/downloadByCalendar", method = RequestMethod.GET)
    @ResponseBody
    public void writeTodayTask(HttpServletRequest request, HttpServletResponse response, Model model, String start, String end) throws UnsupportedEncodingException, ParseException {

        logger.info(">>> [" + request.getSession().getId() + "] Success get the start date: " + start + " and end date: " + end);
        SimpleDateFormat sdf = new SimpleDateFormat( "yyyy-MM-dd");
        Date endDate = new Date();
        endDate = sdf.parse(end);

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(endDate);
        calendar.add(calendar.DATE, +1);
        endDate = calendar.getTime();
        end = sdf.format(endDate);

        ApiReturn ar = new ApiReturn();

        // ------------------------------------------------
        List<TrelloLists> listTrelloLists = (List<TrelloLists>) request.getSession().getAttribute("listTrelloLists");
        Map<String, String> membersMap = (Map<String, String>) request.getSession().getAttribute("membersMap");

        XSSFWorkbook wb = new XSSFWorkbook();

        XSSFCellStyle listTitleStyle = Utility.createCellStyle(wb,(short)12,true,true, false, "PINK", false);
        XSSFCellStyle colNameStyle = Utility.createCellStyle(wb,(short)12,true,true, false, "YELLOW", false);
        XSSFCellStyle taskCenterStyle = Utility.createCellStyle(wb,(short)12,true,false, false, null, false);
        XSSFCellStyle taskDescStyle = Utility.createCellStyle(wb,(short)12,false,false, true, null, false);
        XSSFCellStyle dateStyle = Utility.createCellStyle(wb,(short)12,true,false, true, null, true);

        // 設定儲存格資料
        Sheet sheet = wb.createSheet("NEW");

        int rowIndex = 0;
        Row rowCol = sheet.createRow(rowIndex++);
        String[] colNames = {"Card","項目","說明","已完成","Due Day","Owner","備註"};

        for(int i = 0 ; i < colNames.length ; i++){
            Cell cellCol = rowCol.createCell(i);
            cellCol.setCellStyle(colNameStyle);
            cellCol.setCellValue(colNames[i]);
        }


        for(TrelloLists tl : listTrelloLists){
            logger.info("開始展開分類清單: " + tl.getName() + ", 卡片總數: " + tl.getCards().size());

            for(TrelloCards tc : tl.getCards()){
                String cardName = tc.getName();
                for(int eachCheckLists = 0; eachCheckLists < tc.getCheckLists().size(); eachCheckLists++){
                    for(int eachCheckItems = 0; eachCheckItems < tc.getCheckLists().get(eachCheckLists).getCheckItems().size(); eachCheckItems++){
                        String dueDay = tc.getCheckLists().get(eachCheckLists).getCheckItems().get(eachCheckItems).getDue();
                        if(dueDay != null){
                            // >0 dueDay > start; <0 dueDay < end
                            int resultStart = dueDay.compareTo(start);
                            int resultEnd = dueDay.compareTo(end);
                            if(!(resultStart > 0 && resultEnd < 0)){
                                tc.getCheckLists().get(eachCheckLists).getCheckItems().remove(eachCheckItems);
                                eachCheckItems--;
                                tc.getCheckLists().get(eachCheckLists).setCheckItemsCnt(tc.getCheckLists().get(eachCheckLists).getCheckItemsCnt() - 1);
                                tc.setCheckItemsCnt(tc.getCheckItemsCnt() - 1);
                            }
                        } else{
                            tc.getCheckLists().get(eachCheckLists).getCheckItems().remove(eachCheckItems);
                            eachCheckItems--;
                            tc.getCheckLists().get(eachCheckLists).setCheckItemsCnt(tc.getCheckLists().get(eachCheckLists).getCheckItemsCnt() - 1);
                            tc.setCheckItemsCnt(tc.getCheckItemsCnt() - 1);
                        }
                    }
                }
                if(tc.getCheckItemsCnt() > 1) {
                    CellRangeAddress craCard = new CellRangeAddress(rowIndex, rowIndex + tc.getCheckItemsCnt() - 1, 0, 0);
                    sheet.addMergedRegion(craCard);
                }

                for(TrelloCheckLists tcl : tc.getCheckLists()){
                    String checkListName = tcl.getName();

                    if(tcl.getCheckItemsCnt() > 1) {
                        CellRangeAddress craCheckList = new CellRangeAddress(rowIndex, rowIndex + tcl.getCheckItemsCnt() - 1, 1, 1);
                        sheet.addMergedRegion(craCheckList);
                    }

                    for(TrelloCheckItems tci : tcl.getCheckItems()){
                        String checkItemName = tci.getName();
                        String status = "";
                        if(tci.getState().equals("incomplete")){
                            status = "X";
                        } else if(tci.getState().equals("complete")){
                            status = "V";
                        }
                        String dueDay = "";
                        if(tci.getDue() != null && !tci.getDue().equals("null")){
                            dueDay = tci.getDue().split("T")[0];
                        }
                        Date due = sdf.parse(dueDay);
                        String owner = membersMap.get(tci.getIdMember());
                        Row taskRow = sheet.createRow(rowIndex++);
                        Cell cell1 = taskRow.createCell(0);
                        cell1.setCellStyle(taskDescStyle);
                        cell1.setCellValue(cardName);
                        Cell cell2 = taskRow.createCell(1);
                        cell2.setCellStyle(taskDescStyle);
                        cell2.setCellValue(checkListName);
                        Cell cell3 = taskRow.createCell(2);
                        cell3.setCellStyle(taskDescStyle);
                        cell3.setCellValue(checkItemName);
                        Cell cell4 = taskRow.createCell(3);
                        cell4.setCellStyle(taskCenterStyle);
                        cell4.setCellValue(status);
                        Cell cell5 = taskRow.createCell(4);
                        cell5.setCellStyle(dateStyle);
                        cell5.setCellValue(due);
                        Cell cell6 = taskRow.createCell(5);
                        cell6.setCellStyle(taskCenterStyle);
                        cell6.setCellValue(owner);
                        Cell cell7 = taskRow.createCell(6);
                        cell7.setCellStyle(taskDescStyle);
                        cell7.setCellValue("");
                    }
                }

                // auto filter
                CellRangeAddress c = CellRangeAddress.valueOf("D1:F1");
                sheet.setAutoFilter(c);
            }
        }

        for (int i = 0 ; i < 7 ; i++) {
            sheet.autoSizeColumn(i);
            if(i == 6){
                sheet.setColumnWidth(i, sheet.getColumnWidth(i)*3);
            }
        }

        SimpleDateFormat fileNameSDF = new SimpleDateFormat("MMdd");
        SimpleDateFormat year = new SimpleDateFormat("yyyy");
        Date today = new Date();

        String filename = fileNameSDF.format(today).toString() + "當日專案進度追蹤.xlsx";
        String headerFileName = new String(filename.getBytes(), "ISO8859-1");
        response.setHeader("Content-Disposition", "attachment; filename="+headerFileName);
        OutputStream out = null;
        try{
            out = new BufferedOutputStream(response.getOutputStream());
            wb.write(out);
        }catch (IOException e){
            System.out.println("excel會出錯誤");
        }finally {
            try{
                out.close();
                wb.close();
            } catch (IOException e){
                System.out.println("excel會出錯誤");
            }
        }
    }

    @RequestMapping(value = "/download", method = RequestMethod.GET)
    @ResponseBody
    public void write(HttpServletRequest request, HttpServletResponse response, Model model) throws UnsupportedEncodingException, ParseException {

        logger.info(">>> [" + request.getSession().getId() + "] Start to download all data");

        ApiReturn ar = new ApiReturn();

        // ------------------------------------------------
        List<TrelloLists> listTrelloLists = (List<TrelloLists>) request.getSession().getAttribute("listTrelloLists");
        Map<String, String> membersMap = (Map<String, String>) request.getSession().getAttribute("membersMap");

        XSSFWorkbook wb = new XSSFWorkbook();

        XSSFCellStyle listTitleStyle = Utility.createCellStyle(wb,(short)12,true,true, false, "PINK", false);
        XSSFCellStyle colNameStyle = Utility.createCellStyle(wb,(short)12,true,true, false, "YELLOW", false);
        XSSFCellStyle taskCenterStyle = Utility.createCellStyle(wb,(short)12,true,false, false, null, false);
        XSSFCellStyle taskDescStyle = Utility.createCellStyle(wb,(short)12,false,false, true, null, false);
        XSSFCellStyle dateStyle = Utility.createCellStyle(wb,(short)12,true,false, true, null, true);


        // 設定儲存格資料
        Sheet sheet = wb.createSheet("NEW");

        int rowIndex = 0;
        for(TrelloLists tl : listTrelloLists){
            logger.info("開始展開分類清單: " + tl.getName() + ", 卡片總數: " + tl.getCards().size());
            CellRangeAddress craList = new CellRangeAddress(rowIndex, rowIndex, 0, 5);
            sheet.addMergedRegion(craList);
            Row rowList = sheet.createRow(rowIndex++);
            Cell cellList = rowList.createCell(0);
            cellList.setCellStyle(listTitleStyle);
            cellList.setCellValue(tl.getName());
            Row rowCol = sheet.createRow(rowIndex++);
            String[] colNames = {"Card","項目","說明","已完成","Due Day","Owner","備註"};

            for(int i = 0 ; i < colNames.length ; i++){
                Cell cellCol = rowCol.createCell(i);
                cellCol.setCellStyle(colNameStyle);
                cellCol.setCellValue(colNames[i]);
            }

            for(TrelloCards tc : tl.getCards()){
                String cardName = tc.getName();

                if(tc.getCheckItemsCnt() > 1) {
                    CellRangeAddress craCard = new CellRangeAddress(rowIndex, rowIndex + tc.getCheckItemsCnt() - 1, 0, 0);
                    sheet.addMergedRegion(craCard);
                }

                for(TrelloCheckLists tcl : tc.getCheckLists()){
                    String checkListName = tcl.getName();

                    if(tcl.getCheckItemsCnt() > 1) {
                        CellRangeAddress craCheckList = new CellRangeAddress(rowIndex, rowIndex + tcl.getCheckItemsCnt() - 1, 1, 1);
                        sheet.addMergedRegion(craCheckList);
                    }

                    for(TrelloCheckItems tci : tcl.getCheckItems()){
                        String checkItemName = tci.getName();
                        String status = "";
                        if(tci.getState().equals("incomplete")){
                            status = "X";
                        } else if(tci.getState().equals("complete")){
                            status = "V";
                        }
                        String dueDay = "";
                        if(tci.getDue() != null && !tci.getDue().equals("null")){
                            dueDay = tci.getDue().split("T")[0];
                        }
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                        Date due = new Date();
                        if(dueDay.length() > 0){
                            due = sdf.parse(dueDay);
                        }
                        String owner = membersMap.get(tci.getIdMember());
                        Row taskRow = sheet.createRow(rowIndex++);
                        Cell cell1 = taskRow.createCell(0);
                        cell1.setCellStyle(taskDescStyle);
                        cell1.setCellValue(cardName);
                        Cell cell2 = taskRow.createCell(1);
                        cell2.setCellStyle(taskDescStyle);
                        cell2.setCellValue(checkListName);
                        Cell cell3 = taskRow.createCell(2);
                        cell3.setCellStyle(taskDescStyle);
                        cell3.setCellValue(checkItemName);
                        Cell cell4 = taskRow.createCell(3);
                        cell4.setCellStyle(taskCenterStyle);
                        cell4.setCellValue(status);
                        Cell cell5 = taskRow.createCell(4);
                        cell5.setCellStyle(dateStyle);
                        if(dueDay.length() > 0){
                            cell5.setCellValue(due);
                        } else{
                            cell5.setCellValue(dueDay);
                        }
                        Cell cell6 = taskRow.createCell(5);
                        cell6.setCellStyle(taskCenterStyle);
                        cell6.setCellValue(owner);
                        Cell cell7 = taskRow.createCell(6);
                        cell7.setCellStyle(taskDescStyle);
                        cell7.setCellValue("");
                    }
                }

                // auto filter
                CellRangeAddress c = CellRangeAddress.valueOf("D2:F2");
                sheet.setAutoFilter(c);
            }
        }

        for (int i = 0 ; i < 7 ; i++) {
            sheet.autoSizeColumn(i);
            if(i == 6){
                sheet.setColumnWidth(i, sheet.getColumnWidth(i)*3);
            }
        }

        SimpleDateFormat fileNameSDF = new SimpleDateFormat("MMdd");
        SimpleDateFormat year = new SimpleDateFormat("yyyy");
        Date today = new Date();

        String filename = year.format(today).toString() + "專案進度追蹤" + fileNameSDF.format(today).toString() + ".xlsx";
        String headerFileName = new String(filename.getBytes(), "ISO8859-1");
        response.setHeader("Content-Disposition", "attachment; filename="+headerFileName);
        OutputStream out = null;
        try{
            out = new BufferedOutputStream(response.getOutputStream());
            wb.write(out);
        }catch (IOException e){
            System.out.println("excel會出錯誤");
        }finally {
            try{
                out.close();
                wb.close();
            } catch (IOException e){
                System.out.println("excel會出錯誤");
            }
        }
    }

    @RequestMapping(value = "/searchDT")
    @ResponseBody
    public JSONObject searchDT(HttpServletRequest request, Model model, String member) throws IOException, ParseException {

        logger.info(">>> [" + request.getSession().getId() + "] Success and go to search");

        JSONObject returnJson = new JSONObject();
        List<ExportToCSV> data;
        if(member.equals("")||member.equals("PM")){
            data = trelloExportService.getTrelloDT(request, returnJson, 0, null, null, null);
        } else{
            data = trelloExportService.getTrelloDT(request, returnJson, 0, null, null, member);
        }

        returnJson.put("data", data);
        return returnJson;
    }

    @RequestMapping(value = "/updateComplete")
    @ResponseBody
    public ResponseEntity<String> updateComplete(HttpServletRequest request, Model model, String cardId, String checkItemsId, String status){
        if(status.equals("in")){
            // incomplete
            logger.info(">>> [" + request.getSession().getId() + "] update the checkItemsId: " + checkItemsId + "from complete to incomplete");
        } else{
            logger.info(">>> [" + request.getSession().getId() + "] update the checkItemsId: " + checkItemsId + "from incomplete to complete");
        }

        ApiReturn ar = new ApiReturn();

        String returnStr = new String();
        Properties prop = ConfigLoader.loadConfig("trello.properties");

        //權限相關
        String trelloKey = prop.getProperty("trello.key");
        String trelloToken = prop.getProperty("trello.token");
        String urlParam = "&key=" + trelloKey + "&token=" + trelloToken;


        try{
            returnStr = WebAPI.sendAPI_trello_put("cards/"+cardId+"/checkItem/"+checkItemsId+"?state=" + status + urlParam);
            List<ExportToCSV> data = trelloExportService.getTrelloDT(request, null, 1, checkItemsId, status, null);
            ar.setRetMessage("");
            ar.setRetStatus("Success");
        } catch (Exception e){
            e.printStackTrace();
            ar.setRetMessage(e.getMessage());
            ar.setRetStatus("Exception");
        }
        return new ResponseEntity<String>(JSON.toJSONString(ar), HttpStatus.OK);
    }
}