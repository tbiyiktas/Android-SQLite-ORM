package lib.concurrent;

/**
 * İş-seviyesi iptal semantiği: cancelWork() iptal bayrağı atar,
 * isWorkCancelled() ile zincirin ilerideki aşamaları bayrağı kontrol edebilir.
 */
public interface WorkCancellationAware {
    boolean cancelWork();
    boolean isWorkCancelled();
}
