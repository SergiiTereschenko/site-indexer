package ua.devchallenge.fixture

import rx.observers.TestSubscriber

class RxFixture {

    def static awaitValue(TestSubscriber testSubscriber, expected) {
        testSubscriber.awaitTerminalEvent()
        testSubscriber.assertCompleted()
        testSubscriber.assertNoErrors()
        testSubscriber.assertValue(expected)
        true
    }

    def static awaitValues(TestSubscriber testSubscriber, ... expected) {
        testSubscriber.awaitTerminalEvent()
        testSubscriber.assertCompleted()
        testSubscriber.assertNoErrors()
        testSubscriber.assertValues(expected)
        true
    }

    def static awaitError(TestSubscriber testSubscriber, Class<? extends Exception> exception) {
        testSubscriber.awaitTerminalEvent()
        testSubscriber.assertNotCompleted()
        testSubscriber.assertError(exception)
        true
    }

    def static awaitCompleted(TestSubscriber testSubscriber) {
        testSubscriber.awaitTerminalEvent()
        testSubscriber.assertNoErrors()
        testSubscriber.assertNoValues()
        testSubscriber.assertCompleted()
        true
    }

}
