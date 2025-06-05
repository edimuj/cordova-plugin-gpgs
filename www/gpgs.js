/*
 * cordova-plugin-gpgs
 * Copyright (C) 2025 Exelerus AB
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

/**
 * Google Play Games Services Plugin for Cordova
 * 
 * A modern Cordova plugin for Google Play Games Services v2 API
 * with comprehensive gaming features including:
 * - Authentication
 * - Leaderboards
 * - Achievements
 * - Cloud saves
 * - Friends
 * - Player stats
 * - Events
 * 
 * @author Exelerus AB
 * @version 1.0.0
 * @see https://github.com/edimuj/cordova-plugin-gpgs
 */

var exec = require('cordova/exec');

/* eslint-disable */
// noinspection JSAnnotator

/**
 * @namespace cordova.plugins.GPGS
 */
var GPGS = {
    /**
     * Error codes used by the plugin
     */
    errorCodes: {
        ERROR_CODE_HAS_RESOLUTION: 1,
        ERROR_CODE_NO_RESOLUTION: 2
    },

    /**
     * Check if Google Play Services are available
     * @returns {Promise<boolean|Object>} Promise that resolves with availability status.
     * If services are not available, returns an object with detailed error information:
     * {
     *   available: false,
     *   errorCode: number,
     *   errorString: string,
     *   isUserResolvable: boolean
     * }
     */
    isGooglePlayServicesAvailable: function() {
        return new Promise((resolve, reject) => {
            exec(function(result) {
                // If result is true, services are available
                if (result === true) {
                    resolve(true);
                } 
                // If result is an object, it contains error details
                else if (typeof result === 'object' && result !== null) {
                    resolve(result);
                }
                // Fallback to boolean conversion
                else {
                    resolve(!!result);
                }
            }, reject, 'GPGS', 'isGooglePlayServicesAvailable', []);
        });
    },

    /**
     * Check if user is signed in
     * @returns {Promise<boolean>} Promise that resolves with sign-in status
     */
    isSignedIn: function() {
        return new Promise((resolve, reject) => {
            exec(function(result) {
                // Handle both object and boolean responses
                if (typeof result === 'object' && result !== null) {
                    resolve(!!result.isSignedIn);
                } else {
                    resolve(!!result);
                }
            }, reject, 'GPGS', 'isSignedIn', []);
        });
    },

    /**
     * Sign in to Google Play Games
     * @returns {Promise<void>} Promise that resolves when sign-in is complete
     */
    login: function() {
        return new Promise((resolve, reject) => {
            exec(resolve, reject, 'GPGS', 'login', []);
        });
    },

    /**
     * Unlock an achievement
     * @param {string} achievementId - ID of the achievement to unlock
     * @returns {Promise<void>} Promise that resolves when achievement is unlocked
     */
    unlockAchievement: function(achievementId) {
        return new Promise((resolve, reject) => {
            exec(resolve, reject, 'GPGS', 'unlockAchievement', [achievementId]);
        });
    },

    /**
     * Increment an achievement
     * @param {string} achievementId - ID of the achievement to increment
     * @param {number} numSteps - Number of steps to increment
     * @returns {Promise<void>} Promise that resolves when achievement is incremented
     */
    incrementAchievement: function(achievementId, numSteps) {
        return new Promise((resolve, reject) => {
            exec(resolve, reject, 'GPGS', 'incrementAchievement', [achievementId, numSteps]);
        });
    },

    /**
     * Show achievements UI
     * @returns {Promise<void>} Promise that resolves when UI is closed
     */
    showAchievements: function() {
        return new Promise((resolve, reject) => {
            exec(resolve, reject, 'GPGS', 'showAchievements', []);
        });
    },

    /**
     * Reveal a hidden achievement
     * @param {string} achievementId - ID of the achievement to reveal
     * @returns {Promise<void>} Promise that resolves when achievement is revealed
     */
    revealAchievement: function(achievementId) {
        return new Promise((resolve, reject) => {
            exec(resolve, reject, 'GPGS', 'revealAchievement', [achievementId]);
        });
    },

    /**
     * Set steps in an achievement
     * @param {string} achievementId - ID of the achievement
     * @param {number} steps - Number of steps to set
     * @returns {Promise<void>} Promise that resolves when steps are set
     */
    setStepsInAchievement: function(achievementId, steps) {
        return new Promise((resolve, reject) => {
            exec(resolve, reject, 'GPGS', 'setStepsInAchievement', [achievementId, steps]);
        });
    },

    /**
     * Submit a score to a leaderboard
     * @param {string} leaderboardId - ID of the leaderboard
     * @param {number} score - Score to submit
     * @returns {Promise<void>} Promise that resolves when score is submitted
     */
    submitScore: function(leaderboardId, score) {
        return new Promise((resolve, reject) => {
            exec(resolve, reject, 'GPGS', 'updatePlayerScore', [leaderboardId, score]);
        });
    },

    /**
     * Get player's score from a leaderboard
     * @param {string} leaderboardId - ID of the leaderboard
     * @returns {Promise<number>} Promise that resolves with the player's score
     */
    getPlayerScore: function(leaderboardId) {
        return new Promise((resolve, reject) => {
            exec(resolve, reject, 'GPGS', 'loadPlayerScore', [leaderboardId]);
        });
    },

    /**
     * Show a specific leaderboard
     * @param {string} leaderboardId - ID of the leaderboard to show
     * @returns {Promise<void>} Promise that resolves when UI is closed
     */
    showLeaderboard: function(leaderboardId) {
        return new Promise((resolve, reject) => {
            exec(resolve, reject, 'GPGS', 'showLeaderboard', [leaderboardId]);
        });
    },

    /**
     * Show all leaderboards
     * @returns {Promise<void>} Promise that resolves when UI is closed
     */
    showAllLeaderboards: function() {
        return new Promise((resolve, reject) => {
            exec(resolve, reject, 'GPGS', 'showAllLeaderboards', []);
        });
    },

    /**
     * Show saved games UI
     * @param {Object} options - UI options
     * @param {string} options.title - Title to display
     * @param {boolean} options.allowAddButton - Whether to show "create new" button
     * @param {boolean} options.allowDelete - Whether to allow deletion
     * @param {number} options.maxSnapshots - Maximum number of snapshots to show
     * @returns {Promise<void>} Promise that resolves when UI is closed
     */
    showSavedGames: function(options) {
        return new Promise((resolve, reject) => {
            exec(resolve, reject, 'GPGS', 'showSavedGames', [
                options.title,
                options.allowAddButton,
                options.allowDelete,
                options.maxSnapshots
            ]);
        });
    },

    /**
     * Save game data
     * @param {string} snapshotName - Name of the save
     * @param {Object} data - Data to save
     * @returns {Promise<void>} Promise that resolves when save is complete
     */
    saveGame: function(snapshotName, data) {
        return new Promise((resolve, reject) => {
            exec(resolve, reject, 'GPGS', 'saveGame', [snapshotName, data]);
        });
    },

    /**
     * Load game data
     * @param {string} snapshotName - Name of the save to load
     * @returns {Promise<Object>} Promise that resolves with the saved data
     */
    loadGame: function(snapshotName) {
        return new Promise((resolve, reject) => {
            exec(resolve, reject, 'GPGS', 'loadGameSave', [snapshotName]);
        });
    },

    /**
     * Get list of friends
     * @returns {Promise<Array>} Promise that resolves with array of friend objects
     */
    getFriendsList: function() {
        return new Promise((resolve, reject) => {
            exec(resolve, reject, 'GPGS', 'getFriendsList', []);
        });
    },

    /**
     * Show another player's profile
     * @param {string} playerId - ID of the player
     * @returns {Promise<void>} Promise that resolves when UI is closed
     */
    showPlayerProfile: function(playerId) {
        return new Promise((resolve, reject) => {
            exec(resolve, reject, 'GPGS', 'showAnotherPlayersProfile', [playerId]);
        });
    },

    /**
     * Show player search UI
     * @returns {Promise<void>} Promise that resolves when UI is closed
     */
    showPlayerSearch: function() {
        return new Promise((resolve, reject) => {
            exec(resolve, reject, 'GPGS', 'showPlayerSearch', []);
        });
    },

    /**
     * Get player info
     * @param {string} playerId - ID of the player (optional, defaults to current player)
     * @returns {Promise<Object>} Promise that resolves with player info
     */
    getPlayerInfo: function(playerId) {
        return new Promise((resolve, reject) => {
            exec(resolve, reject, 'GPGS', 'getPlayer', [playerId || '']);
        });
    },

    /**
     * Get current player stats
     * @returns {Promise<Object>} Promise that resolves with player stats
     */
    getPlayerStats: function() {
        return new Promise((resolve, reject) => {
            exec(resolve, reject, 'GPGS', 'getCurrentPlayerStats', []);
        });
    },

    /**
     * Increment an event
     * @param {string} eventId - ID of the event
     * @param {number} amount - Amount to increment
     * @returns {Promise<void>} Promise that resolves when event is incremented
     */
    incrementEvent: function(eventId, amount) {
        return new Promise((resolve, reject) => {
            exec(resolve, reject, 'GPGS', 'incrementEvent', [eventId, amount]);
        });
    },

    /**
     * Get all events
     * @returns {Promise<Array>} Promise that resolves with array of events
     */
    getAllEvents: function() {
        return new Promise((resolve, reject) => {
            exec(resolve, reject, 'GPGS', 'getAllEvents', []);
        });
    },

    /**
     * Get specific event
     * @param {string} eventId - ID of the event
     * @returns {Promise<Object>} Promise that resolves with event data
     */
    getEvent: function(eventId) {
        return new Promise((resolve, reject) => {
            exec(resolve, reject, 'GPGS', 'getEvent', [eventId]);
        });
    }
};

/**
 * Helper function to call cordova plugin
 * @param {String} name - function name to call
 * @param {Array} params - optional params
 * @param {Function} onSuccess - optional on sucess function
 * @param {Function} onFailure - optional on failure functioin
 */
function callPlugin(name, params, onSuccess, onFailure)
{
    cordova.exec(function callPluginSuccess(result)
    {
        if (isFunction(onSuccess))
        {
            onSuccess(result);
        }
    }, function callPluginFailure(error)
    {
        if (isFunction(onFailure))
        {
            onFailure(error)
        }
    }, 'GPGS', name, params);
}

/**
 * Helper function to check if a function is a function
 * @param {Object} functionToCheck - function to check if is function
 */
function isFunction(functionToCheck)
{
    var getType = {};
    var isFunction = functionToCheck && getType.toString.call(functionToCheck) === '[object Function]';
    return isFunction === true;
}

/**
 * Helper function to do a shallow defaults (merge). Does not create a new object, simply extends it
 * @param {Object} o - object to extend
 * @param {Object} defaultObject - defaults to extend o with
 */
function defaults(o, defaultObject)
{
    if (typeof o === 'undefined')
    {
        return defaults({}, defaultObject);
    }

    for (var j in defaultObject)
    {
        if (defaultObject.hasOwnProperty(j) && o.hasOwnProperty(j) === false)
        {
            o[j] = defaultObject[j];
        }
    }

    return o;
}

/**
 * Initialize the plugin and set up event listeners
 * @returns {Promise<void>}
 */
function initialize() {
    return new Promise((resolve, reject) => {
        // Set up event listeners for automatic initialization
        document.addEventListener('gpgs.signin', function(event) {
            const data = event.detail;
            if (data.isSignedIn) {
                console.log('User is signed in');
                // Enable game features
            } else {
                console.log('Sign-in failed:', data.error);
                // Show sign-in button or handle error
            }
        });

        document.addEventListener('gpgs.signout', function(event) {
            const data = event.detail;
            console.log('User signed out:', data.reason);
            // Disable game features that require sign-in
            // Show sign-in button
        });

        document.addEventListener('gpgs.availability', function(event) {
            const data = event.detail;
            if (data.available) {
                console.log('Google Play Services are available');
            } else {
                console.log('Google Play Services are not available:', data.errorString);
                if (data.isUserResolvable) {
                    // Show UI to help user resolve the issue
                    console.log('This issue can be resolved by the user');
                }
            }
        });

        // Check if Google Play Services are available
        exec(
            function(result) {
                if (typeof result === 'boolean') {
                    resolve(result);
                } else {
                    // Handle detailed availability information
                    if (result.available) {
                        resolve(true);
                    } else {
                        reject(new Error(result.errorString));
                    }
                }
            },
            function(error) {
                reject(error);
            },
            'GPGS',
            'isGooglePlayServicesAvailable',
            []
        );
    });
}

module.exports = GPGS;
