package com.boeing.cas.supa.ground;

import java.io.File;

import javax.servlet.http.HttpServletRequest;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.*;
import org.springframework.web.bind.annotation.*;

import com.boeing.cas.supa.ground.controllers.FileUploadController;
import com.boeing.cas.supa.ground.helpers.EmailSender;

@RestController
@EnableAutoConfiguration
@SpringBootApplication
public class DemoApplication {
	@RequestMapping("/")
	String home(HttpServletRequest httpRequest) {
		try {
			System.out.println("sending emial");
			EmailSender.SendMail();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return "Ping!";
	}
	public static void main(String[] args) {
		new File(FileUploadController.UNZIPED_FOLDER).mkdirs();
		new File(FileUploadController.UPLOADED_FOLDER).mkdirs();
		SpringApplication.run(DemoApplication.class, args);
	}
}
