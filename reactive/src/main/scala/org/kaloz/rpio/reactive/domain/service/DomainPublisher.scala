package org.kaloz.rpio.reactive.domain.service

import org.kaloz.rpio.reactive.domain.api._
import rx.lang.scala.Observer
import rx.lang.scala.subjects.PublishSubject

trait RxDomainPublisherImpl extends DomainPublisher {
  val subject = PublishSubject[DomainEvent]()

  override def subscribe(eventOn: PartialFunction[DomainEvent, Unit]): Subscription = {
    val subscription = subject.subscribe(Observer[DomainEvent]((event: DomainEvent) => eventOn(event)))

    new Subscription {
      override def unsubscribe(): Unit = subscription.unsubscribe()
    }
  }

  override def publish(event: DomainEvent): Unit = subject.onNext(event)
}


object DomainPublisher extends RxDomainPublisherImpl

