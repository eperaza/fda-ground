package com.boeing.cas.supa.ground.services;

import com.boeing.cas.supa.ground.helpers.ExcelHelper;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toMap;

@Service
public class UploadService {

    private final ExcelHelper excelHelper;

    private int airlineCellIndex = 2;
    private int emailRowIndex = 4;

    public UploadService(ExcelHelper excelHelper){
        this.excelHelper = excelHelper;
    }

    static boolean isValid(String email) {
        String regex = "^[\\w-_\\.+]*[\\w-_\\.]\\@([\\w]+\\.)+[\\w]+[\\w]$";
        return email.matches(regex);
    }

    public List<Map<String, String>> upload(MultipartFile file) throws Exception {
        Path tempDir = Files.createTempDirectory("");
        File tempFile = tempDir.resolve(file.getOriginalFilename()).toFile();
        file.transferTo(tempFile);

        Workbook workbook = WorkbookFactory.create(tempFile);

        Sheet sheet = workbook.getSheetAt(0);

        Supplier<Stream<Row>> rowStreamSupplier = excelHelper.getRowStreamSupplier(sheet);

        Row airlineRow = rowStreamSupplier.get().findFirst().get();
        List<String> airlineCells = excelHelper.getStream(airlineRow)
                .map(Cell::getStringCellValue)
                .map(String::valueOf)
                .collect(Collectors.toList());

        // **IMPORTANT
        String airline = airlineCells.get(airlineCellIndex);
        System.out.println("airline: " + airline);

        Row headerRow = rowStreamSupplier.get().skip(1).findFirst().get();

        List<String> headerCells = excelHelper.getStream(headerRow)
                .map(Cell::getStringCellValue)
                .map(String::valueOf)
                .collect(Collectors.toList());

        System.out.println("****header CElls: ");
        for(String cell: headerCells){
            System.out.println(cell);
        }

        int colCount = headerCells.size();


        List<String> emails = rowStreamSupplier.get().skip(2).map(row -> {
            return row.getCell(emailRowIndex).getStringCellValue();
        }).collect(Collectors.toList());

        List<String> invalidEmails = new ArrayList<>();
        for(String email: emails){
            if(!isValid(email)){
                System.out.println("Invalid email found! - " + email);
                invalidEmails.add(email);
            }
        }
        if(invalidEmails.size() == 0){
            System.out.println("All Emails Good!!!");
        }

        List<Map<String, String>> result = rowStreamSupplier.get()
                .skip(1)
                .map(row -> {
                    List<String> cellList = excelHelper.getStream(row)
                            .map(Cell::getStringCellValue)
                            .collect(Collectors.toList());

                    return excelHelper.cellIteratorSupplier(colCount)
                            .get()
                            .collect(toMap(headerCells::get, cellList::get));
                })
                .collect(Collectors.toList());

        return result;
    }
}