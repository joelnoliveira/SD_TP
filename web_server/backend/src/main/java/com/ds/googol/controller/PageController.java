package com.ds.googol.controller;

import gateway_client_dependencies.*;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpSession;

import java.util.UUID;

import org.apache.catalina.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;
// Anotations
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.ui.Model;

import com.ds.googol.beans.Number;




@Controller
public class PageController {
    
	private Number id_counter = new Number();

	@Resource(name = "sessionScopedNumberGenerator")
    private Number nSession;


	@Bean
    @Scope(value = WebApplicationContext.SCOPE_SESSION, proxyMode = ScopedProxyMode.TARGET_CLASS)
    public Number sessionScopedNumberGenerator() {
		Number n = new Number();
		n.setN(id_counter.next());
        return n;
    }
	
    @GetMapping("/index")
	public String index() {
		
		return "index";
	}

	@PostMapping("/index")
	public String index(@RequestParam("option") String option, Model model) {
		
		System.out.println(option);
		return "index";
	}

	@GetMapping("/counter")
	public String counter(Model model) {
		System.out.println(nSession.getN());
		return "index";
	}
}