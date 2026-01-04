package com.darkrockstudios.texteditor.find

/**
 * Localizable strings for the FindBar component.
 * Provide a custom implementation to localize the Find UI.
 */
data class FindBarStrings(
	val placeholder: String,
	val clearSearch: String,
	val noMatches: String,
	val previousMatch: String,
	val nextMatch: String,
	val close: String,
	/**
	 * Format string for match count display.
	 * Will be called with (currentIndex, totalCount) parameters.
	 * Example: "3 of 15"
	 */
	val matchCount: (current: Int, total: Int) -> String,
	val replacePlaceholder: String,
	val replace: String,
	val replaceAll: String,
	val showReplace: String,
	val hideReplace: String,
) {
	companion object {
		/**
		 * Default English strings for FindBar.
		 */
		val Default = FindBarStrings(
			placeholder = "Find...",
			clearSearch = "Clear search",
			noMatches = "No matches",
			previousMatch = "Prev",
			nextMatch = "Next",
			close = "Close",
			matchCount = { current, total -> "$current of $total" },
			replacePlaceholder = "Replace with...",
			replace = "Replace",
			replaceAll = "All",
			showReplace = "Replace",
			hideReplace = "Hide",
		)
	}
}
