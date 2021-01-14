package com.smb.trelloExport.service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.smb.trelloExport.bean.*;
import com.smb.trelloExport.utility.ConfigLoader;
import com.smb.trelloExport.utility.Constant;
import com.smb.trelloExport.utility.WebAPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.io.*;
import java.text.ParseException;
import java.util.*;

@Service
public class TrelloExportService {

    private static Logger logger = LoggerFactory.getLogger(TrelloExportService.class);

    public List<ExportToCSV> getTrelloDT(HttpServletRequest request, JSONObject returnJson, Integer previous, String needUpdateCheckItemsId, String checkItemsStatus, String member) throws IOException, ParseException {
        List<TrelloLists> listTrelloLists;
        Map<String, String> membersMap = null;

        String returnStr = new String();
        Properties prop = ConfigLoader.loadConfig("trello.properties");
        //權限相關
        String trelloKey = prop.getProperty("trello.key");
        String trelloToken = prop.getProperty("trello.token");
        String urlParam = "?key=" + trelloKey + "&token=" + trelloToken;

        if(request.getSession().getAttribute("listTrelloLists") != null && request.getSession().getAttribute("membersMap") != null){
            listTrelloLists = (List<TrelloLists>) request.getSession().getAttribute("listTrelloLists");
            membersMap = (Map<String, String>) request.getSession().getAttribute("membersMap");
        } else{

            // 人員名稱對應
            String[] showUserName = prop.getProperty("excel.showUserName").split(",");
            String[] trelloName = prop.getProperty("trello.fullName").split(",");

            //源頭board
            String boardID = prop.getProperty("trello.board");

            //取得人員對照表
            membersMap = new HashMap<String, String>();
            returnStr = WebAPI.sendAPI_trello("boards/"+boardID+"/members"+urlParam);
            List<TrelloMembers> listTrelloMembers = JSONArray.parseArray(returnStr, TrelloMembers.class);
            for(TrelloMembers tm : listTrelloMembers){
                for(int eachTrelloName = 0; eachTrelloName < trelloName.length; eachTrelloName++){
                    if(tm.getFullName().toUpperCase().contains(trelloName[eachTrelloName])){
                        membersMap.put(tm.getId(), showUserName[eachTrelloName]);
                    }
                }
            }

            //取得分類列表
            returnStr = WebAPI.sendAPI_trello("boards/"+boardID+"/lists"+urlParam);
            listTrelloLists = JSONArray.parseArray(returnStr, TrelloLists.class);

            //取得列表底下 Card
            for(TrelloLists tl : listTrelloLists){
                logger.debug(tl.toString());
                returnStr = WebAPI.sendAPI_trello("lists/"+tl.getId()+"/cards"+urlParam);
                List<TrelloCards> listTrelloCards = JSONArray.parseArray(returnStr, TrelloCards.class);
                for(TrelloCards tc : listTrelloCards){
                    logger.debug(tc.toString());
                    List<TrelloCheckLists> listTrelloCheckLists = new ArrayList<TrelloCheckLists>();
                    int accCheckItemsCnt = 0;
                    for(String icl : tc.getIdCheckLists()){
                        returnStr = WebAPI.sendAPI_trello("checklists/"+icl+urlParam);
                        TrelloCheckLists tcl = JSONObject.parseObject(returnStr, TrelloCheckLists.class);
                        tcl.setCheckItemsCnt(tcl.getCheckItems().size());
                        accCheckItemsCnt += tcl.getCheckItemsCnt();
                        listTrelloCheckLists.add(tcl);
                    }
                    tc.setCheckLists((ArrayList<TrelloCheckLists>) listTrelloCheckLists);
                    tc.setCheckItemsCnt(accCheckItemsCnt);
                }
                tl.setCards((ArrayList<TrelloCards>) listTrelloCards);
            }
            request.getSession().setAttribute("listTrelloLists", listTrelloLists);
            request.getSession().setAttribute("membersMap", membersMap);
        }

        // 展開
        List<ExportToCSV> data = new ArrayList<ExportToCSV>();
        Integer total = 0;
        for(TrelloLists tl : listTrelloLists){
            logger.info("開始展開分類清單: " + tl.getName() + ", 卡片總數: " + tl.getCards().size());
            for(TrelloCards tc : tl.getCards()){
                String cardId = tc.getId();
                String cardName = tc.getName();
                for(TrelloCheckLists tcl : tc.getCheckLists()){
                    String checkListName = tcl.getName();
                    for(TrelloCheckItems tci : tcl.getCheckItems()){
                        String checkItemsId = tci.getId();
                        if(previous == 1){
                            // 先比對checkItemsId 再更新
                            if(tci.getId().equals(needUpdateCheckItemsId)){
                                tci.setState(checkItemsStatus);
                            }

                        }

                        if(member == null || member.equals("PM")){
                            // get all data
                            ExportToCSV dataNumber = new ExportToCSV();

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
                            String owner = membersMap.get(tci.getIdMember());

                            // save each data and return to web
                            dataNumber.setId(total);
                            dataNumber.setCardName(cardName);
                            dataNumber.setCheckListName(checkListName);
                            dataNumber.setCheckItemName(checkItemName);
                            dataNumber.setCheckItemState(status);
                            dataNumber.setCheckItemDue(dueDay);
                            dataNumber.setIdMember(owner);
                            dataNumber.setCardId(cardId);
                            dataNumber.setCheckItemsId(checkItemsId);
                            data.add(dataNumber);
                            total ++;
                        } else if( tci.getIdMember() != null && member.equals(Constant.NAME_MAP.get(membersMap.get(tci.getIdMember()))) ){
                            // get select member data
                            ExportToCSV dataNumber = new ExportToCSV();

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
                            String owner = membersMap.get(tci.getIdMember());

                            // save each data and return to web
                            dataNumber.setId(total);
                            dataNumber.setCardName(cardName);
                            dataNumber.setCheckListName(checkListName);
                            dataNumber.setCheckItemName(checkItemName);
                            dataNumber.setCheckItemState(status);
                            dataNumber.setCheckItemDue(dueDay);
                            dataNumber.setIdMember(owner);
                            dataNumber.setCardId(cardId);
                            dataNumber.setCheckItemsId(checkItemsId);
                            data.add(dataNumber);
                            total ++;
                        }
                    }
                }
            }
        }
        return data;
    }
}
