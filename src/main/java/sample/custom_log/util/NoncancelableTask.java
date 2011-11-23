package sample.custom_log.util;

public abstract class NoncancelableTask {
    /**
     * endure(long)がtrueを戻すか、指定したmillisを過ぎるまで、残りの時間を引数に指定してendure()を呼び続ける。
     * @param millis ミリ秒。0以下を指定した場合何もせずfalseを戻す。
     * @return endure(long)がtrueを戻した場合true、指定時間が経過した場合falseを戻す。
     */
    public final boolean runWithLimit(long millis) {
        if (millis <= 0) {
            return false;
        }
        long endTime = System.currentTimeMillis() + millis;

        long rest = millis;
        boolean interrupted = false;
        try {
            do {
                try {
                    if (endure(rest)) {
                        return true;
                    }
                } catch (InterruptedException e) {
                    interrupted = true;
                }
            } while ((rest = endTime - System.currentTimeMillis()) > 0);
            return false;
        } finally {
            if (interrupted) {
                Thread.currentThread().interrupt();
            }
        }
    }
    /**
     */
    public final boolean runWithoutLimit(long interval) {
        boolean interrupted = false;
        try {
            while (true) {
                try {
                    if (endure(interval)) {
                        return true;
                    }
                } catch (InterruptedException e) {
                    interrupted = true;
                }
            }
        } finally {
            if (interrupted) {
                Thread.currentThread().interrupt();
            }
        }
    }
    /**
     * 一定時間かかり、途中でInterruptedExceptionが呼ばれるような処理を実装する。
     * 処理は、何度でも呼び出し可能である必要がある。
     * @param millis 残りミリ秒。常に1以上。
     * @return　処理を中断するときはtrueを、続行するときはfalseを戻す。
     * @throws InterruptedException 割り込みによる中断があった場合、InterruptedExceptionをそのまま投げる。
     */
    protected abstract boolean endure(long millis) throws InterruptedException;
}