package com.eternalcode.parcellockers.command.argument;

import com.eternalcode.parcellockers.parcel.Parcel;
import dev.rollczi.litecommands.argument.ArgumentName;
import dev.rollczi.litecommands.argument.simple.OneArgument;
import dev.rollczi.litecommands.command.LiteInvocation;
import dev.rollczi.litecommands.suggestion.Suggestion;
import panda.std.Result;

import java.util.List;

@ArgumentName("parcel")
public class ParcelArgument implements OneArgument<Parcel> {

	// TODO: Parse parcel from argument
	@Override
	public Result<Parcel, ?> parse(LiteInvocation invocation, String argument) {
		return null;
	}

	// TODO: Fetch parcels from database
	@Override
	public List<Suggestion> suggest(LiteInvocation invocation) {
		return OneArgument.super.suggest(invocation);
	}
}
