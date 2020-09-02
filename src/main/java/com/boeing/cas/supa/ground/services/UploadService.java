package com.boeing.cas.supa.ground.services;

import com.boeing.cas.supa.ground.controllers.AirlineFocalAdminController;
import com.boeing.cas.supa.ground.helpers.ExcelHelper;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import org.apache.poi.ss.usermodel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
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
    private final Logger logger = LoggerFactory.getLogger(AirlineFocalAdminController.class);
    private int airlineCellIndex = 1;
    private int emailRowIndex = 4;

    public UploadService(ExcelHelper excelHelper){
        this.excelHelper = excelHelper;
    }

    static boolean isValid(String email) {
        boolean result = true;
        try {
            InternetAddress emailAddr = new InternetAddress(email);
            emailAddr.validate();
        } catch (AddressException ex) {
            result = false;
        }
        return result;
    }

    public List<Map<String, String>> upload(MultipartFile file) throws Exception {
        Path tempDir = Files.createTempDirectory("");
        File tempFile = tempDir.resolve(file.getOriginalFilename()).toFile();
        file.transferTo(tempFile);
        try{
            Workbook workbook = WorkbookFactory.create(tempFile);

            Sheet sheet = workbook.getSheetAt(0);

            Supplier<Stream<Row>> rowStreamSupplier = excelHelper.getRowStreamSupplier(sheet);

            boolean isRowPresent = rowStreamSupplier.get().findFirst().isPresent();
            boolean isHeaderPesent = rowStreamSupplier.get().skip(1).findFirst().isPresent();

            if(isRowPresent && isHeaderPesent){
                Row airlineRow = rowStreamSupplier.get().findFirst().get();
                List<String> airlineCells = excelHelper.getStream(airlineRow)
                        .map(Cell::getStringCellValue)
                        .map(String::valueOf)
                        .collect(Collectors.toList());
                // **IMPORTANT
                String airline = airlineCells.get(airlineCellIndex);
                logger.debug("AIRLINE: " + airline);

                Row headerRow = rowStreamSupplier.get().skip(1).findFirst().get();

                List<String> headerCells = excelHelper.getStream(headerRow)
                        .map(Cell::getStringCellValue)
                        .map(String::toLowerCase)
                        .collect(Collectors.toList());

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
                        logger.debug("!!! INVALID EMAIL : " + email);
                        invalidEmails.add(email);
                        throw new Exception("Invalid Email in ExcelFile");
                    }
                }
                if(invalidEmails.size() == 0){
                    logger.debug("All Emails Checkout!!! =) ");
                }

                List<Map<String, String>> result = rowStreamSupplier.get()
                        .skip(2)
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

        }catch(InvalidFormatException ex){
            logger.debug("Something went horribly wrong with Upload: {}", ex);
        }
        return null;
    }
}