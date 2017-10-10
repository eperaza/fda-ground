package com.boeing.cas.supa.ground;

import java.io.File;

import javax.servlet.http.HttpServletRequest;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.*;
import org.springframework.web.bind.annotation.*;

import com.boeing.cas.supa.ground.controllers.FileUploadController;

@RestController
@EnableAutoConfiguration
@SpringBootApplication
public class DemoApplication {
	@RequestMapping("/")
	String home(HttpServletRequest httpRequest) {
		return "Ping!";
	}
	public static void main(String[] args) {

		new File(FileUploadController.UPLOADED_FOLDER).mkdirs();
		SpringApplication.run(DemoApplication.class, args);
	}
}
