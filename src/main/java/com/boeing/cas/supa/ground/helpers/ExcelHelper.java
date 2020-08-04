package com.boeing.cas.supa.ground.helpers;

import org.apache.poi.ss.usermodel.Row;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.function.Supplier;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@Component
public class ExcelHelper {
    public static String TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    static String[] HEADERs = { "Id", "Title", "Description", "Published" };
    static String SHEET = "Tutorials";

    public static boolean hasExcelFormat(MultipartFile file) {
        if (!TYPE.equals(file.getContentType())) {
            return false;
        }
        return true;
    }

    public Supplier<Stream<Row>> getRowStreamSupplier(Iterable<Row> rows){
        return () -> getStream(rows);
    }

    public <T> Stream<T> getStream(Iterable<T> iterable){
        return StreamSupport.stream(iterable.spliterator(), false);
    }

    public Supplier<Stream<Integer>> cellIteratorSupplier(int end) {
        return () -> numberStream(end);
    }

    public Stream<Integer> numberStream(int end){
        return IntStream.range(0, end).boxed();
    }
}
