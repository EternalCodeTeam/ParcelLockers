package com.eternalcode.parcellockers.user;

import java.util.List;

public record UserPageResult(List<User> users, boolean hasNextPage) {

}
