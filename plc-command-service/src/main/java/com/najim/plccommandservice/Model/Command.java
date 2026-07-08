package com.najim.plccommandservice.Model;

public record Command(
        String commandId,
        String machineId,
        String action
) {
}
