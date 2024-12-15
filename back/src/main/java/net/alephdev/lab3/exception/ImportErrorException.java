package net.alephdev.lab3.exception;

import lombok.Getter;
import net.alephdev.lab3.models.User;

@Getter
public class ImportErrorException extends RuntimeException {
    User user;

    public ImportErrorException(String message, User user) {
        super(message);
        this.user = user;
    }
}
