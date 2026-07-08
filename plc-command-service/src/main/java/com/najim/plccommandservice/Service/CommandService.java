package com.najim.plccommandservice.Service;

import com.najim.plccommandservice.Model.Command;
import org.springframework.stereotype.Service;

import java.util.Random;

@Service
public class CommandService {

    public String executeCommand(Command command) {
        Random random = new Random();
        int num = random.nextInt(10);
        if (num<3){
            throw new RuntimeException("plc unreachable");

        }
        return new String("\"Command executed: \" "+ command.action());
    }
}
