package com.unicornt.store.infrastructure.persistence.adapter;

import com.unicornt.store.infrastructure.persistence.repository.SpringDataStockRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("StockRepositoryAdapter")
class StockRepositoryAdapterTest {

    @Mock private SpringDataStockRepository stock;
    @InjectMocks private StockRepositoryAdapter adapter;

    @Test
    @DisplayName("true when the conditional update touched a row, false otherwise")
    void translatesRowCount() {
        when(stock.decreaseStock(2, 3)).thenReturn(1);
        when(stock.decreaseStock(2, 999)).thenReturn(0);

        assertThat(adapter.decreaseStock(2L, 3)).isTrue();
        assertThat(adapter.decreaseStock(2L, 999)).isFalse();
    }
}
