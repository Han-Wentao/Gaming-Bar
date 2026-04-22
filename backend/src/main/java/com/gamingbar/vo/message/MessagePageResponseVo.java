package com.gamingbar.vo.message;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class MessagePageResponseVo {

    private Boolean hasMore;
    private Long nextCursor;
    private List<MessageVo> messages;
}
