package com.gamingbar.common.result;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PageData<T> {

    private long total;
    private int page;
    private int size;
    private List<T> list;
}
