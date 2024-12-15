package net.alephdev.lab3.models;

import jakarta.persistence.*;
import lombok.*;
import net.alephdev.lab3.enums.ImportStatus;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImportHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ImportStatus status;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "created_by_id")
    private User createdBy;

    @Column(nullable = false)
    private int count;

    private String filename;
}
