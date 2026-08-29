package com.najim.plccommandservice;

import com.najim.plccommandservice.Model.Command;
import com.najim.plccommandservice.Service.CommandService;
import org.junit.jupiter.api.RepeatedTest;
import static org.junit.jupiter.api.Assertions.*;

class CommandServiceTest {

    @RepeatedTest(20)
    void executeCommandEitherSucceedsOrThrows() {
        CommandService service = new CommandService();
        Command command = new Command("cmd-1", "compressor-1", "reduce-speed");

        try {
            String result = service.executeCommand(command);
            assertTrue(result.contains("Command executed"));
        } catch (RuntimeException e) {
            assertEquals("plc unreachable", e.getMessage());
        }
    }
}