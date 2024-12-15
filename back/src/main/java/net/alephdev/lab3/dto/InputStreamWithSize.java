package net.alephdev.lab3.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.core.io.InputStreamSource;

@Getter
@AllArgsConstructor
public class InputStreamWithSize {
    private InputStreamSource inputStreamSource;
    private long size;
}
