package com.compute.rental.common.page;

import java.util.List;

public record PageResult<T>(
        List<T> records,
        long total,
        long pageNo,
        long pageSize
) {
}
