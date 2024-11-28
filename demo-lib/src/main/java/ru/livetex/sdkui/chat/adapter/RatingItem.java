package ru.livetex.sdkui.chat.adapter;

import java.util.Objects;

import ru.livetex.sdk.entity.DialogRatingType;

public class RatingItem implements Comparable, AdapterItem {
	public String textBefore  = "";
	public String textAfter  = "";
	public boolean commentEnabled = false;
	public String comment = "";
	public int rating5 = 0;
	public Boolean rating2 = null;
	public boolean isSet = false;
	public DialogRatingType type = DialogRatingType.FIVE_POINT;

	public RatingItem(String textBefore, String textAfter, boolean commentEnabled, int rating5, Boolean rating2, String comment,
					  boolean isSet, DialogRatingType type) {
		this.textBefore = textBefore;
		this.textAfter = textAfter;
		this.commentEnabled = commentEnabled;
		this.rating5 = rating5;
		this.rating2 = rating2;
		this.comment = comment;
		this.isSet = isSet;
		this.type = type;
	}

	@Override
	public ItemType getAdapterItemType() {
		return ItemType.RATING;
	}

	@Override
	public int compareTo(Object o) {
		if (o instanceof RatingItem) {
			// shouldn't be more than 1 in list
			return 0;
		}
		return 0;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		RatingItem that = (RatingItem) o;
		return commentEnabled == that.commentEnabled && rating5 == that.rating5 && isSet == that.isSet && Objects.equals(textBefore, that.textBefore) && Objects.equals(textAfter, that.textAfter) && Objects.equals(comment, that.comment) && Objects.equals(rating2, that.rating2) && type == that.type;
	}

	@Override
	public int hashCode() {
		return Objects.hash(textBefore, textAfter, commentEnabled, comment, rating5, rating2, isSet, type);
	}
}
