package com.space.userautomation.services;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.io.*;
import java.util.*;
public class ExcelReader {
    public static Map<String,Object> readExcelSheet(InputStream input, String Filename) {
        List<List<String>> contentList = new ArrayList<>();
        List<String> headers = new ArrayList<>();
        Map<String, Object> allData = new HashMap<>();
        try {
            String strFileExtn = Filename.substring(Filename.lastIndexOf(".") + 1);
            if (strFileExtn.equalsIgnoreCase("xlsx")) {
                Workbook workbook = new XSSFWorkbook(input);
                int iNumOfSheets = workbook.getNumberOfSheets();
                for (int sheetIndex = 0; sheetIndex < iNumOfSheets; sheetIndex++)
                {
                    int headerRow = 0;
                    int dataLength=0;
                    Sheet datatypeSheet = workbook.getSheetAt(sheetIndex);
                    Iterator<Row> iterator = datatypeSheet.iterator();
                    List<String> sheetData = null;
                    while (iterator.hasNext()) {
                        sheetData = new ArrayList();
                        Row currentRow = iterator.next();
                        Iterator<Cell> cellIterator = currentRow.iterator();
                        if (headerRow == 0) {
                            while (cellIterator.hasNext()) {
                                Cell currentCell = cellIterator.next();
                                if (currentCell != null) {
                                    XSSFCell dataCell = (XSSFCell) currentCell;
                                    if (!(dataCell.toString()).isEmpty()) {
                                        headers.add(dataCell.toString());
                                        dataLength++;
                                    }
                                }
                            }
                        }
                        else {
                            for(int cellNo=0; cellNo<dataLength; cellNo++)
                            {
                                Cell currentCell = currentRow.getCell(cellNo);
                                if (currentCell != null) {
                                    XSSFCell dataCell = (XSSFCell) currentCell;
                                    if (!dataCell.toString().isEmpty()) {
                                        sheetData.add(dataCell.toString());
                                    } else {
                                        sheetData.add("");
                                    }
                                } else {
                                    sheetData.add("");
                                }
                            }
                        }
                        if (headerRow != 0)
                            contentList.add(sheetData);
                        headerRow++;
                    }
                }
            }
        } catch(FileNotFoundException e){
            e.printStackTrace();
            System.exit(0);
        } catch(IOException e){
            e.printStackTrace();
            System.exit(0);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(0);
        }
        allData.put("header",headers);
        allData.put("data",contentList);
        return allData;
    }
}
