import kotlin.concurrent.thread
import kotlin.random.Random

// Число потоков, из которых будут поступать "события скролла"
const val ThreadsCount = 2

/**
 * Простая модель события, обрабатываемого главным потоком.
 */
data class ScrollEvent(val name: String)

/**
 * Имитация "сложной системы", которая позволяет добавлять слушателей (listener) на событие скролла.
 */
object FakeSystem {

    // Общий список всех "слушателей"
    private val listeners = mutableListOf<ScrollEventListener>()

    /**
     * Метод для отправки события скролла.
     * Метод вызывается на разных потоках != главному.
     *
     * При отправке события оно рассылается всем слушателям.
     */
    fun sendScrollEvent(scrollEvent: ScrollEvent) {
        println("Send scroll event from Thread-${Thread.currentThread().name}")
        for (listener in listeners) {
            listener.handleScrollEvent(scrollEvent)
        }
    }

    /**
     * Метод для добавления нового слушателя в систему.
     */
    fun registerScrollEventListener(listener: ScrollEventListener) {
        this.listeners += listener
    }

    // Интерфейс "слушателя" событий скролла
    fun interface ScrollEventListener {
        fun handleScrollEvent(scrollEvent: ScrollEvent)
    }

}

/**
 * Стартовая точка всей программы - запускает несколько потоков, которые отправляют событие скролла в главный поток
 * через объект "системы".
 */
fun main(args: Array<String>) {
    // Запускаем потоки
    for (i in 1..ThreadsCount) {
        thread(start = true) {
            while (true) {
                // Отправляем событие из потока Thread #$i
                FakeSystem.sendScrollEvent(ScrollEvent("Thread #$i"))

                // Добавляем случайную паузу перед отправкой следующего события.
                val sleepTimeInMills = Random.nextLong(from = 1, until = 20) * 1000
                Thread.sleep(sleepTimeInMills)
            }
        }
    }

    println("Name of the main thread: ${Thread.currentThread().name}")
    // Запускаем главный цикл
    MainLooper().startLoop()
}

/**
 * Наша собственная реализация Looper
 */
class MainLooper {

    private var isRunning = true
    private val tasksQueue = mutableListOf<Runnable>()

    fun startLoop() {
        FakeSystem.registerScrollEventListener { scrollEvent ->
            println("${Thread.currentThread().name} : Looper catch new scrollEvent from another thread")
            tasksQueue += Runnable { doSmth(scrollEvent) }

            synchronized(lock = tasksQueue) {
                println("${Thread.currentThread().name} : Notify main thread!")
                (tasksQueue as Object).notify()
            }
        }

        while (isRunning) {
            println("[${Thread.currentThread().name}] : Cycle of endless 'while (isRunning)'")
            doWork()
        }
    }

    private fun doWork() {
        println("[${Thread.currentThread().name}] : do work (is task queue not empty: ${tasksQueue.isNotEmpty()}")
        if (tasksQueue.isNotEmpty()) {
            val currentTask = tasksQueue[0]
            currentTask.run()
            tasksQueue.remove(currentTask)
        } else {
            println("[${Thread.currentThread().name}] : queue is empty -> go to sleep")
            synchronized(lock = tasksQueue) {
                println("[${Thread.currentThread().name}] : before 'wait()'")
                (tasksQueue as Object).wait()
            }
        }
    }

    private fun doSmth(scrollEvent: ScrollEvent) {
        println("[${Thread.currentThread().name}] : Do some work with scroll event (${scrollEvent}))")
    }

}