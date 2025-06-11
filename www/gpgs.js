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
                    resolve(result.isAvailable);
                }
                // Fallback to boolean conversion
                else {
                    resolve(result);
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
                    resolve(result.isSignedIn);
                } else {
                    resolve(result);
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
     * Load all achievements for the current player
     * @param {boolean} forceReload - Whether to force a reload from the server
     * @returns {Promise<Array>} Promise that resolves with an array of achievement objects
     */
    loadAchievements: function(forceReload) {
        return new Promise((resolve, reject) => {
            exec(resolve, reject, 'GPGS', 'loadAchievements', [forceReload || false]);
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
     * Load top scores for a leaderboard
     * @param {string} leaderboardId - ID of the leaderboard
     * @param {number} timeSpan - Time span (0=daily, 1=weekly, 2=all_time)
     * @param {number} collection - Collection (0=public, 1=social)
     * @param {number} maxResults - Max number of scores to return
     * @returns {Promise<Object>} Promise that resolves with leaderboard scores
     */
    loadTopScores: function(leaderboardId, timeSpan, collection, maxResults) {
        return new Promise((resolve, reject) => {
            exec(resolve, reject, 'GPGS', 'loadTopScores', [leaderboardId, timeSpan, collection, maxResults]);
        });
    },

    /**
     * Load scores centered around the player
     * @param {string} leaderboardId - ID of the leaderboard
     * @param {number} timeSpan - Time span (0=daily, 1=weekly, 2=all_time)
     * @param {number} collection - Collection (0=public, 1=social)
     * @param {number} maxResults - Max number of scores to return
     * @returns {Promise<Object>} Promise that resolves with leaderboard scores
     */
    loadPlayerCenteredScores: function(leaderboardId, timeSpan, collection, maxResults) {
        return new Promise((resolve, reject) => {
            exec(resolve, reject, 'GPGS', 'loadPlayerCenteredScores', [leaderboardId, timeSpan, collection, maxResults]);
        });
    },

    /**
     * Load leaderboard metadata
     * @param {string} [leaderboardId] - ID of the leaderboard (optional, loads all if not provided)
     * @returns {Promise<Object|Array>} Promise that resolves with leaderboard metadata
     */
    loadLeaderboardMetadata: function(leaderboardId) {
        return new Promise((resolve, reject) => {
            exec(resolve, reject, 'GPGS', 'loadLeaderboardMetadata', leaderboardId ? [leaderboardId] : []);
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
     * @param {string} description - Description of the save
     * @param {Object} data - Data to save
     * @returns {Promise<void>} Promise that resolves when save is complete
     */
    saveGame: function(snapshotName, description, data) {
        return new Promise((resolve, reject) => {
            exec(resolve, reject, 'GPGS', 'saveGame', [snapshotName, description, data]);
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
     * Delete a snapshot
     * @param {string} snapshotName - Name of the snapshot to delete
     * @returns {Promise<string>} Promise that resolves with the snapshot ID on success
     */
    deleteSnapshot: function(snapshotName) {
        return new Promise((resolve, reject) => {
            exec(resolve, reject, 'GPGS', 'deleteSnapshot', [snapshotName]);
        });
    },

    /**
     * Load all snapshots for the current player
     * @param {boolean} forceReload - Whether to force a reload from the server
     * @returns {Promise<Array>} Promise that resolves with an array of snapshot metadata objects
     */
    loadAllSnapshots: function(forceReload) {
        return new Promise((resolve, reject) => {
            exec(resolve, reject, 'GPGS', 'loadAllSnapshots', [forceReload || false]);
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
    },

    /**
     * Initialize the plugin and perform silent sign-in.
     * This must be called once by the app before using authenticated features.
     * @returns {Promise<void>} Promise that resolves when initialization request is sent.
     */
    initialize: function() {
        return new Promise((resolve, reject) => {
            exec(resolve, reject, 'GPGS', 'initialize', []);
        });
    }
};

module.exports = GPGS;
