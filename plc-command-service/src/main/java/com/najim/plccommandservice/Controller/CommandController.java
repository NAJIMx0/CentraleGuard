package com.najim.plccommandservice.Controller;

import com.najim.plccommandservice.Model.Command;
import com.najim.plccommandservice.Service.CommandService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/plc")
@RequiredArgsConstructor

public class CommandController {

    private final CommandService commandService;

    @PostMapping("/command")
    public ResponseEntity<String> sendCommand(@RequestBody Command command) {
        commandService.executeCommand(command);
        return ResponseEntity.ok("Success") ;
    }
}
