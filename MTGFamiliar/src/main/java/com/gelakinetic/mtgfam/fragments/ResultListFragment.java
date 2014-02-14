package com.gelakinetic.mtgfam.fragments;

import android.database.Cursor;
import android.database.MergeCursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.Toast;

import com.gelakinetic.mtgfam.FamiliarActivity;
import com.gelakinetic.mtgfam.R;
import com.gelakinetic.mtgfam.helpers.CardDbAdapter;
import com.gelakinetic.mtgfam.helpers.FamiliarDbException;
import com.gelakinetic.mtgfam.helpers.ResultListAdapter;
import com.gelakinetic.mtgfam.helpers.SearchCriteria;

import java.util.ArrayList;
import java.util.Random;

/**
 * This fragment displays a list of cards. It can be from a search result, some MoJhoSto basic, or whatever
 */
public class ResultListFragment extends FamiliarFragment {

	/* The cursor with the data and the list view to display it */
	private Cursor mCursor;
	private ListView mListView;
	/* Static integers preserve list position during the fragment's lifecycle */
	private static int mCursorPosition;
	private static int mCursorPositionOffset;

	/* Database must be kept open as long as the cursor needs it */
	private CardDbAdapter mDbHelper;

	/* Constants for bundled arguments */
	public static final String CARD_ID_0 = "id0";
	public static final String CARD_ID_1 = "id1";
	public static final String CARD_ID_2 = "id2";

	/**
	 * When the fragment is created, open the database and search for whatever.
	 * This should likely be done off the UI thread, but it's usually a quick operation,
	 * and the user wouldn't be doing anything else anyway
	 *
	 * @param savedInstanceState If the fragment is being re-created from a previous saved state, this is the state.
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		long id;

		/* After a search, make sure the position is on top */
		mCursorPosition = 0;
		mCursorPositionOffset = 0;

		/* All the things we may want to display */
		String[] returnTypes = new String[]{CardDbAdapter.KEY_ID, CardDbAdapter.KEY_NAME, CardDbAdapter.KEY_SET,
				CardDbAdapter.KEY_RARITY, CardDbAdapter.KEY_MANACOST, CardDbAdapter.KEY_TYPE, CardDbAdapter.KEY_ABILITY,
				CardDbAdapter.KEY_POWER, CardDbAdapter.KEY_TOUGHNESS, CardDbAdapter.KEY_LOYALTY,
				CardDbAdapter.KEY_NUMBER};

		Bundle args = this.getArguments();

		/* Open up the database, search for stuff */
		try {
			mDbHelper = new CardDbAdapter(this.getActivity());

			/* If "id0" exists, then it's three cards and they should be merged
			 * Otherwise, do a search with the given criteria
			 */
			if ((id = args.getLong(CARD_ID_0)) != 0L) {
				long id1 = args.getLong(CARD_ID_1);
				long id2 = args.getLong(CARD_ID_2);
				Cursor cs[] = new Cursor[3];
				cs[0] = mDbHelper.fetchCard(id, null);
				cs[1] = mDbHelper.fetchCard(id1, null);
				cs[2] = mDbHelper.fetchCard(id2, null);
				mCursor = new MergeCursor(cs);
			}
			else {
				SearchCriteria criteria = (SearchCriteria) args.getSerializable(SearchViewFragment.CRITERIA);
				assert criteria != null; /* Because Android Studio */
				boolean consolidate = (criteria.setLogic == CardDbAdapter.MOSTRECENTPRINTING ||
						criteria.setLogic == CardDbAdapter.FIRSTPRINTING);

				mCursor = mDbHelper.Search(criteria.name, criteria.text, criteria.type, criteria.color,
						criteria.colorLogic, criteria.set, criteria.powChoice, criteria.powLogic, criteria.touChoice,
						criteria.touLogic, criteria.cmc, criteria.cmcLogic, criteria.format, criteria.rarity,
						criteria.flavor, criteria.artist, criteria.typeLogic, criteria.textLogic, criteria.setLogic,
						true, returnTypes, consolidate);
			}

			if (this.isAdded()) {
				if (mCursor == null || mCursor.getCount() == 0) {
					Toast.makeText(this.getActivity(), getFragmentManager().getBackStackEntryCount() + " " +
							getString(R.string.search_toast_no_results),
							Toast.LENGTH_SHORT).show();
					if (!getActivity().isTaskRoot()) {
						getActivity().finish();
					}
					else {
						getFragmentManager().popBackStack();
					}
				}
				else if (mCursor.getCount() == 1) {
					mCursor.moveToFirst();
					id = mCursor.getLong(mCursor.getColumnIndex(CardDbAdapter.KEY_ID));
					startCardViewFrag(id);
				}
				else {
					Toast.makeText(this.getActivity(), String.format(getString(R.string.search_toast_results),
							mCursor.getCount()), Toast.LENGTH_LONG).show();
				}
			}
		} catch (FamiliarDbException e) {
			handleFamiliarDbException(true);
		}
	}

	/**
	 * Be clean with the cursor!
	 */
	@Override
	public void onDestroy() {
		super.onDestroy();
		if (mCursor != null) {
			mCursor.close();
		}
		if (mDbHelper != null) {
			mDbHelper.close();
		}
	}

	/**
	 * Save the cursor position when the fragment is paused so that it can be restored in onResume()
	 */
	@Override
	public void onPause() {
		super.onPause();
		mCursorPosition = mListView.getFirstVisiblePosition();
		View cursorPositionView = mListView.getChildAt(0);
		mCursorPositionOffset = (cursorPositionView == null) ? 0 : cursorPositionView.getTop();
	}

	/**
	 * When the fragment resumes, fill mListView with mCursor, and move the selection to its prior state, so that the
	 * list doesn't appear to jump around when opening new fragments
	 */
	@Override
	public void onResume() {
		super.onResume();
		fillData();
		mListView.setSelectionFromTop(mCursorPosition, mCursorPositionOffset);
	}

	/**
	 * When the view is created, set up the ListView. The data will be filled in onResume
	 *
	 * @param inflater           The LayoutInflater object that can be used to inflate any views in the fragment,
	 * @param container          If non-null, this is the parent view that the fragment's UI should be attached to. The
	 *                           fragment should not add the view itself, but this can be used to generate the
	 *                           LayoutParams of the view.
	 * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state as given
	 *                           here.
	 * @return the view if the fragment is showing, null if otherwise
	 */
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

		if (container == null) {
			/* Something is happening when the fragment is on the back stack */
			return null;
		}

		/* Inflate the view */
		View myFragmentView = inflater.inflate(R.layout.result_list_frag, container, false);
		assert myFragmentView != null; /* Because Android Studio */
		mListView = (ListView) myFragmentView.findViewById(R.id.resultList);

		Bundle res = ((FamiliarActivity) getActivity()).getFragmentResults();
		if (res != null) {
			if (mCursor.getCount() == 1) {
				/* Jump back past the result list (it wasn't displayed because this card is a singleton) */
				if (!getActivity().isTaskRoot()) {
					getActivity().finish();
				}
				else {
					getFragmentManager().popBackStack();
				}
			}
		}

		mListView.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				startCardViewFrag(id);
			}
		});

		return myFragmentView;
	}

	/**
	 * This function fills mListView with the info in mCursor using a ResultListAdapter
	 */
	private void fillData() {
		if (mCursor != null) {
			ArrayList<String> fromList = new ArrayList<String>();
			ArrayList<Integer> toList = new ArrayList<Integer>();
			fromList.add(CardDbAdapter.KEY_NAME);
			toList.add(R.id.cardname);
			if (((FamiliarActivity) getActivity()).mPreferenceAdapter.getSetPref()) {
				fromList.add(CardDbAdapter.KEY_SET);
				toList.add(R.id.cardset);
			}
			if (((FamiliarActivity) getActivity()).mPreferenceAdapter.getManaCostPref()) {
				fromList.add(CardDbAdapter.KEY_MANACOST);
				toList.add(R.id.cardcost);
			}
			if (((FamiliarActivity) getActivity()).mPreferenceAdapter.getTypePref()) {
				fromList.add(CardDbAdapter.KEY_TYPE);
				toList.add(R.id.cardtype);
			}
			if (((FamiliarActivity) getActivity()).mPreferenceAdapter.getAbilityPref()) {
				fromList.add(CardDbAdapter.KEY_ABILITY);
				toList.add(R.id.cardability);
			}
			if (((FamiliarActivity) getActivity()).mPreferenceAdapter.getPTPref()) {
				fromList.add(CardDbAdapter.KEY_POWER);
				toList.add(R.id.cardp);
				fromList.add(CardDbAdapter.KEY_TOUGHNESS);
				toList.add(R.id.cardt);
				fromList.add(CardDbAdapter.KEY_LOYALTY);
				toList.add(R.id.cardt);
			}

			String[] from = new String[fromList.size()];
			fromList.toArray(from);

			int[] to = new int[toList.size()];
			for (int i = 0; i < to.length; i++) {
				to[i] = toList.get(i);
			}

			ResultListAdapter rla = new ResultListAdapter(getActivity(), mCursor, from, to);
			mListView.setAdapter(rla);
		}
	}

	/**
	 * Convenience method to start a card view fragment.
	 *
	 * @param id The id of the card to display, or -1 for a random card
	 */
	private void startCardViewFrag(long id) {
		Bundle args = new Bundle();
		int cardPosition = 0;

		/* Build the array of ids sequentially, make note of the chosen card's position */
		long cardIds[] = new long[mCursor.getCount()];
		mCursor.moveToFirst();
		for (int i = 0; i < mCursor.getCount(); i++, mCursor.moveToNext()) {
			cardIds[i] = mCursor.getLong(mCursor.getColumnIndex(CardDbAdapter.KEY_ID));
			if (cardIds[i] == id) {
				cardPosition = i;
			}
		}

		if (id == -1) {
			Random rand = new Random(System.currentTimeMillis());

			/* Shuffle the array of ids */
			/* implements http://en.wikipedia.org/wiki/Fisher-Yates_shuffle */
			long temp;
			int k, j;
			for (k = cardIds.length - 1; k > 0; k--) {
				j = rand.nextInt(k + 1);/* j = random integer with 0 <= j <= i */
				temp = cardIds[j];
				cardIds[j] = cardIds[k];
				cardIds[k] = temp;
			}

			/* Start at the beginning of the random sequence */
			cardPosition = 0;
		}

		/* Load the array of ids and position into the bundle, start the fragment */
		args.putInt(CardViewPagerFragment.STARTING_CARD_POSITION, cardPosition);
		args.putLongArray(CardViewPagerFragment.CARD_ID_ARRAY, cardIds);
		CardViewPagerFragment cardViewPagerFragment = new CardViewPagerFragment();
		startNewFragment(cardViewPagerFragment, args);
	}

	/**
	 * Create an options menu. Super will handle adding a SearchView
	 *
	 * @param menu     The options menu in which you place your items.
	 * @param inflater The inflater to use to inflate the menu
	 */
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		inflater.inflate(R.menu.result_list_menu, menu);
	}

	/**
	 * Handle an ActionBar item click
	 *
	 * @param item the item clicked
	 * @return true if the click was acted on
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		/* Handle item selection */
		switch (item.getItemId()) {
			case R.id.search_menu_random_search:
				startCardViewFrag(-1);
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}
}