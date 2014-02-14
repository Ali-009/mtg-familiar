/**
 Copyright 2011 Adam Feinstein

 This file is part of MTG Familiar.

 MTG Familiar is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 MTG Familiar is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with MTG Familiar.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.gelakinetic.mtgfam.helpers;

import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.text.Html.ImageGetter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AlphabetIndexer;
import android.widget.SectionIndexer;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import com.gelakinetic.mtgfam.R;

import org.jetbrains.annotations.NotNull;

/**
 * This list adapter is used to display a list of search results. It implements SectionIndexer to enable fast scrolling.
 */
@SuppressWarnings("ALL")
public class ResultListAdapter extends SimpleCursorAdapter implements SectionIndexer {

	private final String[] mFrom;
	private final int[] mTo;
	private final Resources mResources;
	private final ImageGetter mImgGetter;
	private final AlphabetIndexer mAlphaIndexer;

	/**
	 * Standard Constructor.
	 *
	 * @param context The context where the ListView associated with this SimpleListItemFactory is running
	 * @param cursor  The database cursor. Can be null if the cursor is not available yet.
	 * @param from    A list of column names representing the data to bind to the UI. Can be null if the cursor is not
	 *                available yet.
	 * @param to      The views that should display column in the "from" parameter. These should all be TextViews. The
	 *                first N views in this list are given the values of the first N columns in the from parameter.
	 *                Can be null if the cursor is not available yet.
	 */
	public ResultListAdapter(Context context, Cursor cursor, String[] from, int[] to) {
		super(context, R.layout.card_row, cursor, from, to, 0);
		this.mFrom = from;
		this.mTo = to;
		this.mResources = context.getResources();
		this.mImgGetter = ImageGetterHelper.GlyphGetter(this.mResources);
		this.mAlphaIndexer = new AlphabetIndexer(cursor, cursor.getColumnIndex(CardDbAdapter.KEY_NAME),
				"ABCDEFGHIJKLMNOPQRSTUVWXYZ");
	}

	/**
	 * Inflates view(s) from the specified XML file.
	 *
	 * @param context Interface to application's global information
	 * @param cursor  The cursor from which to get the data. The cursor is already moved to the correct position.
	 * @param parent  The parent to which the new view is attached to
	 * @return the inflated view
	 */
	@Override
	public View newView(Context context, Cursor cursor, ViewGroup parent) {
		final LayoutInflater inflater = LayoutInflater.from(context);
		return inflater.inflate(R.layout.card_row, parent, false);
	}

	/**
	 * Binds all of the field names passed into the "to" parameter of the constructor with their corresponding cursor
	 * columns as specified in the "from" parameter. Binding occurs in two phases. First, if a
	 * SimpleCursorAdapter.ViewBinder is available, setViewValue(android.view.View, android.database.Cursor, int) is
	 * invoked. If the returned value is true, binding has occurred. If the returned value is false and the view to bind
	 * is a TextView, setViewText(TextView, String) is invoked. If the returned value is false and the view to bind is
	 * an ImageView, setViewImage(ImageView, String) is invoked. If no appropriate binding can be found, an
	 * IllegalStateException is thrown.
	 *
	 * @param view    Existing view, returned earlier by newView
	 * @param context Interface to application's global information
	 * @param cursor  The cursor from which to get the data. The cursor is already moved to the correct position.
	 */
	@Override
	public void bindView(@NotNull View view, Context context, @NotNull Cursor cursor) {

		boolean hideCost = true;
		boolean hideSet = true;
		boolean hideType = true;
		boolean hideAbility = true;
		boolean hidePT = true;
		boolean hideLoyalty = true;

        /* make sure these elements are showing (views get recycled) */
		view.findViewById(R.id.cardp).setVisibility(View.VISIBLE);
		view.findViewById(R.id.cardslash).setVisibility(View.VISIBLE);
		view.findViewById(R.id.cardt).setVisibility(View.VISIBLE);

        /* Iterate through the mFrom, find the appropriate view in mTo */
		for (int i = 0; i < mFrom.length; i++) {

			TextView textField = (TextView) view.findViewById(mTo[i]);

			if (CardDbAdapter.KEY_NAME.equals(mFrom[i])) {
				String name = cursor.getString(cursor.getColumnIndex(mFrom[i]));
				textField.setText(name);
			}
			else if (CardDbAdapter.KEY_MANACOST.equals(mFrom[i])) {
				String name = cursor.getString(cursor.getColumnIndex(mFrom[i]));
				hideCost = false;
				CharSequence csq = ImageGetterHelper.formatStringWithGlyphs(name, mImgGetter);
				textField.setText(csq);
			}
			else if (CardDbAdapter.KEY_SET.equals(mFrom[i])) {
				String name = cursor.getString(cursor.getColumnIndex(mFrom[i]));
				hideSet = false;
				textField.setText(name);
				char rarity = (char) cursor.getInt(cursor.getColumnIndex(CardDbAdapter.KEY_RARITY));
				switch (rarity) {
					case 'C':
						textField.setTextColor(mResources.getColor(R.color.common));
						break;
					case 'U':
						textField.setTextColor(mResources.getColor(R.color.uncommon));
						break;
					case 'R':
						textField.setTextColor(mResources.getColor(R.color.rare));
						break;
					case 'M':
						textField.setTextColor(mResources.getColor(R.color.mythic));
						break;
					case 'T':
						textField.setTextColor(mResources.getColor(R.color.timeshifted));
						break;
				}
			}
			else if (CardDbAdapter.KEY_TYPE.equals(mFrom[i])) {
				String name = cursor.getString(cursor.getColumnIndex(mFrom[i]));
				hideType = false;
				textField.setText(name);
			}
			else if (CardDbAdapter.KEY_ABILITY.equals(mFrom[i])) {
				String name = cursor.getString(cursor.getColumnIndex(mFrom[i]));
				hideAbility = false;
				CharSequence csq = ImageGetterHelper.formatStringWithGlyphs(name, mImgGetter);
				textField.setText(csq);
			}
			else if (CardDbAdapter.KEY_POWER.equals(mFrom[i])) {
				float p = cursor.getFloat(cursor.getColumnIndex(mFrom[i]));
				if (p != CardDbAdapter.NOONECARES) {
					String pow;
					hidePT = false;
					if (p == CardDbAdapter.STAR)
						pow = "*";
					else if (p == CardDbAdapter.ONEPLUSSTAR)
						pow = "1+*";
					else if (p == CardDbAdapter.TWOPLUSSTAR)
						pow = "2+*";
					else if (p == CardDbAdapter.SEVENMINUSSTAR)
						pow = "7-*";
					else if (p == CardDbAdapter.STARSQUARED)
						pow = "*^2";
					else {
						if (p == (int) p) {
							pow = Integer.valueOf((int) p).toString();
						}
						else {
							pow = Float.valueOf(p).toString();
						}
					}
					textField.setText(pow);
				}
			}
			else if (CardDbAdapter.KEY_TOUGHNESS.equals(mFrom[i])) {
				float t = cursor.getFloat(cursor.getColumnIndex(mFrom[i]));
				if (t != CardDbAdapter.NOONECARES) {
					hidePT = false;
					String tou;
					if (t == CardDbAdapter.STAR)
						tou = "*";
					else if (t == CardDbAdapter.ONEPLUSSTAR)
						tou = "1+*";
					else if (t == CardDbAdapter.TWOPLUSSTAR)
						tou = "2+*";
					else if (t == CardDbAdapter.SEVENMINUSSTAR)
						tou = "7-*";
					else if (t == CardDbAdapter.STARSQUARED)
						tou = "*^2";
					else {
						if (t == (int) t) {
							tou = Integer.valueOf((int) t).toString();
						}
						else {
							tou = Float.valueOf(t).toString();
						}
					}
					textField.setText(tou);
				}
			}
			else if (CardDbAdapter.KEY_LOYALTY.equals(mFrom[i])) {
				float l = cursor.getFloat(cursor.getColumnIndex(mFrom[i]));
				if (l != CardDbAdapter.NOONECARES) {
					hideLoyalty = false;
					if (l == (int) l) {
						textField.setText(Integer.toString((int) l));
					}
					else {
						textField.setText(Float.toString(l));
					}
				}
			}
		}

		/* Hide the fields if they should be hidden (didn't exist in mTo)*/
		if (hideCost) {
			view.findViewById(R.id.cardcost).setVisibility(View.GONE);
		}
		if (hideSet) {
			view.findViewById(R.id.cardset).setVisibility(View.GONE);
		}
		if (hideType) {
			view.findViewById(R.id.cardtype).setVisibility(View.GONE);
		}
		if (hideAbility) {
			view.findViewById(R.id.cardability).setVisibility(View.GONE);
		}
		if (!hideLoyalty) {
			view.findViewById(R.id.cardp).setVisibility(View.GONE);
			view.findViewById(R.id.cardslash).setVisibility(View.GONE);
		}
		else if (hidePT) {
			view.findViewById(R.id.cardp).setVisibility(View.GONE);
			view.findViewById(R.id.cardslash).setVisibility(View.GONE);
			view.findViewById(R.id.cardt).setVisibility(View.GONE);
		}
	}

	/**
	 * Given the index of a section within the array of section objects, returns the starting position of that section
	 * within the adapter. If the section's starting position is outside of the adapter bounds, the position must be
	 * clipped to fall within the size of the adapter.
	 *
	 * @param section the index of the section within the array of section objects
	 * @return the starting position of that section within the adapter, constrained to fall within the adapter bounds
	 */
	public int getPositionForSection(int section) {
		return mAlphaIndexer.getPositionForSection(section); /* use the indexer */
	}

	/**
	 * Given a position within the adapter, returns the index of the corresponding section within the array of section
	 * objects. If the section index is outside of the section array bounds, the index must be clipped to fall within
	 * the size of the section array. For example, consider an indexer where the section at array index 0 starts at
	 * adapter position 100. Calling this method with position 10, which is before the first section, must return index
	 * 0.
	 *
	 * @param position the position within the adapter for which to return the corresponding section index
	 * @return the index of the corresponding section within the array of section objects, constrained to fall within
	 * the array bounds
	 */
	public int getSectionForPosition(int position) {
		return mAlphaIndexer.getSectionForPosition(position); /* use the indexer */
	}

	/**
	 * Returns an array of objects representing sections of the list. The returned array and its contents should be
	 * non-null. The list view will call toString() on the objects to get the preview text to display while scrolling.
	 * For example, an adapter may return an array of Strings representing letters of the alphabet. Or, it may return an
	 * array of objects whose toString() methods return their section titles.
	 *
	 * @return the array of section objects
	 */
	public Object[] getSections() {
		return mAlphaIndexer.getSections(); /* use the indexer */
	}
}