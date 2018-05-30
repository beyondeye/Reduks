package com.beyondeye.reduks

/**
 * interface for marking an action as something that is processed ONLY by some Saga, should be
 * ignored by normal reducer: you can use this for two purposes:
 * 1) write reducers with the first case of when() expression that match a SagaAction and ignore it (for better perfomance)
 * 2) when sending an action to a MultiStore, that need only to be processed to some saga attached to the MultiStore, not
 *    by any of its substore: NOTE that in this case you will actually get an exception by trying to dispatch an action without
 *    context that is not marked as SagaAction: see MultiStore.dispatchWrappedAction()
 */
interface SagaAction