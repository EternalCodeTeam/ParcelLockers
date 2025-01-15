package com.eternalcode.parcellockers.user.repository;

import com.eternalcode.parcellockers.user.User;

import java.util.List;

public record UserPageResult(List<User> users, boolean hasNextPage) {

}
