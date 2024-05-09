package woowacourse.movie.presentation.homefragments.reservationList

import android.util.Log
import woowacourse.movie.database.ReservationDatabase
import woowacourse.movie.model.Seat
import woowacourse.movie.model.Theater
import woowacourse.movie.model.Ticket
import woowacourse.movie.presentation.homefragments.reservationList.uiModel.ReservationItemUiModel
import woowacourse.movie.repository.DummyTheaterList
import woowacourse.movie.repository.TheaterListRepository

class ReservationListPresenter(
    private val view: ReservationListContract.View,
    private val theaterListRepository: TheaterListRepository = DummyTheaterList,
) : ReservationListContract.Presenter {
    private var _reservationList: MutableList<ReservationItemUiModel> = mutableListOf()
    val reservationList: List<ReservationItemUiModel>
        get() = _reservationList.toList()

    override fun loadReservations(reservationDatabase: ReservationDatabase) {
        refreshDb(reservationDatabase)
        view.displayReservations(reservationList)
    }

    private fun refreshDb(reservationDatabase: ReservationDatabase) {
        Thread {
            val dao = reservationDatabase.reservationDao()
            dao.getAll().map { reservationData ->
                _reservationList.add(
                    ReservationItemUiModel(
                        id = reservationData.id,
                        theaterName = findTheater(reservationData.theaterId)?.name,
                        movieTitle = reservationData.movieTitle,
                        screenDate = reservationData.screenDate,
                        screenTime = reservationData.screenTime,
                    ),
                )
            }
        }.start()
    }

    override fun navigate(
        reservationId: Long,
        reservationDatabase: ReservationDatabase,
    ) {
        val ticket: Ticket? = findTicket(reservationId, reservationDatabase)
        Log.d("navigate", "$ticket")
        if (ticket == null) {
            view.showErrorToast()
        } else {
            view.navigate(ticket)
        }
    }

    private fun findTicket(
        reservationId: Long,
        reservationDatabase: ReservationDatabase,
    ): Ticket? {
        var ticket: Ticket? = null
        val t =
            Thread {
                val dao = reservationDatabase.reservationDao()
                val data = dao.selectWithId(reservationId)
                Log.d("reservationPresenter", "data found")
                ticket =
                    Ticket(
                        movieTitle = data.movieTitle,
                        screeningDateTime = "${data.screenDate} ${data.screenTime}",
                        selectedSeats = data.selectedSeats.toSeatList(),
                        theaterId = data.theaterId,
                    )
                Log.d("ticket", "$ticket")
            }
        t.start()
        t.join()
        Log.d("ticket(out of thread)", "$ticket")
        return ticket
    }

    private fun findTheater(theaterId: Long): Theater? {
        return theaterListRepository.findTheaterOrNull(theaterId)
    }

    private fun String.toSeatList(): List<Seat> {
        val stringSeatList = this.split(", ")
        return stringSeatList.map {
            Seat(
                it[0] - 'A',
                it[1].digitToInt() - 1,
            )
        }
    }
}