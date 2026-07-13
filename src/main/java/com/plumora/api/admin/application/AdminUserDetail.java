package com.plumora.api.admin.application;

import com.plumora.api.user.domain.User;

public record AdminUserDetail(User user, long booksCount, long reportsCount) {
}
