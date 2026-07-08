package com.najim.plccommandservice.Controller;

import com.najim.plccommandservice.Service.CommandService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/plc")
@RequiredArgsConstructor

public class CommandController {

    private final CommandService commandService;

    @PostMapping("/command")
    public String sendCommand(@RequestParam String command) {
    }
}
