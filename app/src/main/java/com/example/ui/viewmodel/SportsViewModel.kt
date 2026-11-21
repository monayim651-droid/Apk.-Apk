package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.IccRankingRow
import com.example.data.model.LeaderStatRow
import com.example.data.model.LeagueStandingRow
import com.example.data.model.MatchDetail
import com.example.data.model.MatchStatus
import com.example.data.model.MatchSummary
import com.example.data.model.PlayerProfile
import com.example.data.model.SportType
import com.example.data.model.SportsNewsItem
import com.example.data.model.TeamProfile
import com.example.data.model.UiState
import com.example.data.repository.SportsRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SportsViewModel(
    private val repository: SportsRepository = SportsRepository()
) : ViewModel() {

    private val _selectedSport = MutableStateFlow(SportType.FOOTBALL)
    val selectedSport: StateFlow<SportType> = _selectedSport.asStateFlow()

    private val _selectedLeague = MutableStateFlow("ALL")
    val selectedLeague: StateFlow<String> = _selectedLeague.asStateFlow()

    private val _statusFilter = MutableStateFlow<MatchStatus?>(null)
    val statusFilter: StateFlow<MatchStatus?> = _statusFilter.asStateFlow()

    private val _matchesState = MutableStateFlow<UiState<List<MatchSummary>>>(UiState.Loading)
    val matchesState: StateFlow<UiState<List<MatchSummary>>> = _matchesState.asStateFlow()

    private val _standingsState = MutableStateFlow<UiState<List<LeagueStandingRow>>>(UiState.Loading)
    val standingsState: StateFlow<UiState<List<LeagueStandingRow>>> = _standingsState.asStateFlow()

    private val _iccRankings = MutableStateFlow<List<IccRankingRow>>(emptyList())
    val iccRankings: StateFlow<List<IccRankingRow>> = _iccRankings.asStateFlow()

    private val _leaders = MutableStateFlow<List<LeaderStatRow>>(emptyList())
    val leaders: StateFlow<List<LeaderStatRow>> = _leaders.asStateFlow()

    private val _newsState = MutableStateFlow<UiState<List<SportsNewsItem>>>(UiState.Loading)
    val newsState: StateFlow<UiState<List<SportsNewsItem>>> = _newsState.asStateFlow()

    private val _selectedMatchDetail = MutableStateFlow<UiState<MatchDetail>?>(null)
    val selectedMatchDetail: StateFlow<UiState<MatchDetail>?> = _selectedMatchDetail.asStateFlow()

    private val _teamProfileState = MutableStateFlow<UiState<TeamProfile>?>(null)
    val teamProfileState: StateFlow<UiState<TeamProfile>?> = _teamProfileState.asStateFlow()

    private val _compareTeamA = MutableStateFlow<TeamProfile?>(null)
    val compareTeamA: StateFlow<TeamProfile?> = _compareTeamA.asStateFlow()

    private val _compareTeamB = MutableStateFlow<TeamProfile?>(null)
    val compareTeamB: StateFlow<TeamProfile?> = _compareTeamB.asStateFlow()

    private val _playerProfileState = MutableStateFlow<UiState<PlayerProfile>?>(null)
    val playerProfileState: StateFlow<UiState<PlayerProfile>?> = _playerProfileState.asStateFlow()

    private val _comparePlayerA = MutableStateFlow<PlayerProfile?>(null)
    val comparePlayerA: StateFlow<PlayerProfile?> = _comparePlayerA.asStateFlow()

    private val _comparePlayerB = MutableStateFlow<PlayerProfile?>(null)
    val comparePlayerB: StateFlow<PlayerProfile?> = _comparePlayerB.asStateFlow()

    val leagues = repository.footballLeagues

    val apiDiagnostics: SportsRepository.ApiDiagnostics
        get() = repository.diagnostics

    init {
        refreshAllForCurrentSport()
    }

    fun selectSport(sport: SportType) {
        if (_selectedSport.value != sport) {
            _selectedSport.value = sport
            _statusFilter.value = null
            refreshAllForCurrentSport()
        }
    }

    fun selectLeague(leagueCode: String) {
        _selectedLeague.value = leagueCode
        loadMatches()
        if (leagueCode != "ALL") {
            loadStandings(leagueCode)
            loadLeaders(leagueCode)
        }
    }

    fun setStatusFilter(status: MatchStatus?) {
        _statusFilter.value = status
    }

    fun refreshAllForCurrentSport() {
        loadMatches()
        loadNews()
        if (_selectedSport.value == SportType.FOOTBALL) {
            val league = if (_selectedLeague.value == "ALL") "eng.1" else _selectedLeague.value
            loadStandings(league)
            loadLeaders(league)
        } else {
            loadIccRankings()
        }
    }

    fun loadMatches() {
        viewModelScope.launch {
            _matchesState.value = UiState.Loading
            try {
                val list = if (_selectedSport.value == SportType.FOOTBALL) {
                    repository.fetchFootballMatches(_selectedLeague.value)
                } else {
                    repository.fetchCricketMatches()
                }
                _matchesState.value = UiState.Success(list)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _matchesState.value = UiState.Error(e.localizedMessage ?: "Failed to load live scores")
            }
        }
    }

    fun loadStandings(leagueCode: String) {
        viewModelScope.launch {
            _standingsState.value = UiState.Loading
            try {
                _standingsState.value = UiState.Success(repository.fetchLeagueStandings(leagueCode))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _standingsState.value = UiState.Error(e.localizedMessage ?: "Failed to load standings")
            }
        }
    }

    private fun loadLeaders(leagueCode: String) {
        viewModelScope.launch {
            try {
                _leaders.value = repository.fetchLeagueLeaders(leagueCode)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _leaders.value = emptyList()
            }
        }
    }

    private fun loadIccRankings() {
        viewModelScope.launch {
            try {
                _iccRankings.value = repository.fetchIccRankingsFromApi()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _iccRankings.value = emptyList()
            }
        }
    }

    fun loadNews() {
        viewModelScope.launch {
            _newsState.value = UiState.Loading
            try {
                _newsState.value = UiState.Success(repository.fetchSportsNews(_selectedSport.value))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _newsState.value = UiState.Error(e.localizedMessage ?: "Failed to load news")
            }
        }
    }

    fun openMatchDetail(summary: MatchSummary) {
        viewModelScope.launch {
            _selectedMatchDetail.value = UiState.Loading
            try {
                val detail = repository.fetchMatchDetail(summary)
                _selectedMatchDetail.value = UiState.Success(detail)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _selectedMatchDetail.value = UiState.Error(e.localizedMessage ?: "Failed to load match details")
            }
        }
    }

    fun openTeamProfile(teamName: String, sport: SportType = _selectedSport.value) {
        viewModelScope.launch {
            _teamProfileState.value = UiState.Loading
            try {
                val profile = repository.searchTeamProfile(teamName, sport)
                if (profile != null) {
                    _teamProfileState.value = UiState.Success(profile)
                } else {
                    _teamProfileState.value = UiState.Error("Team profile not found in TheSportsDB for \"$teamName\".")
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _teamProfileState.value = UiState.Error(e.localizedMessage ?: "Failed to load team profile")
            }
        }
    }

    fun openPlayerProfile(playerName: String, sport: SportType = _selectedSport.value) {
        viewModelScope.launch {
            _playerProfileState.value = UiState.Loading
            try {
                val profile = repository.searchPlayerProfile(playerName, sport)
                if (profile != null) {
                    _playerProfileState.value = UiState.Success(profile)
                } else {
                    _playerProfileState.value = UiState.Error("Player profile not found in TheSportsDB for \"$playerName\".")
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _playerProfileState.value = UiState.Error(e.localizedMessage ?: "Failed to load player profile")
            }
        }
    }

    fun loadComparisonTeams(teamAQuery: String, teamBQuery: String) {
        viewModelScope.launch {
            try {
                _compareTeamA.value = repository.searchTeamProfile(teamAQuery, _selectedSport.value)
                _compareTeamB.value = repository.searchTeamProfile(teamBQuery, _selectedSport.value)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {}
        }
    }

    fun loadComparisonPlayers(playerAQuery: String, playerBQuery: String) {
        viewModelScope.launch {
            try {
                _comparePlayerA.value = repository.searchPlayerProfile(playerAQuery, _selectedSport.value)
                _comparePlayerB.value = repository.searchPlayerProfile(playerBQuery, _selectedSport.value)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {}
        }
    }
}
