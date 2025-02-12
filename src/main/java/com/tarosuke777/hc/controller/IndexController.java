package com.tarosuke777.hc.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller

public class IndexController {

	@GetMapping("/")
	public String index(@RequestParam(name = "channelId", defaultValue = "1") String channelId, Model model) {
		model.addAttribute("channelId", channelId);
		return "index";
	}

}