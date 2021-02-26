package github.jojo;

import lombok.*;

/**
 * @author zzj
 * @version 1.0
 * @date 2021/1/30 15:30
 * @description
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@ToString
public class Hello {
    private String message;
    private String description;
}
