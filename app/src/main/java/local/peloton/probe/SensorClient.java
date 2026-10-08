package local.peloton.probe;

import android.content.*;
import android.os.*;
import android.util.Log;

/** Owner-device interoperability client. Only callback registration and unregistration are sent. */
final class SensorClient implements AutoCloseable {
  private static final String PACKAGE = "com.onepeloton.affernetservice";
  private static final String INTERFACE = PACKAGE + ".IV1Interface";
  private static final String CALLBACK = PACKAGE + ".IV1Callback";

  static final class Reading {
    final long rpm, time;
    final double watts, mph;
    final int resistance;

    Reading(long rpm, long rawPower, int resistance) {
      this.rpm = rpm;
      this.watts = BikeMath.watts(rawPower);
      this.mph = BikeMath.speedMph(watts);
      this.resistance = resistance;
      time = SystemClock.elapsedRealtime();
    }
  }

  private final Context context;
  private final HandlerThread worker = new HandlerThread("BikeTube sensors");
  private final Handler work;
  private boolean bound, closed;
  private IBinder remote;
  private volatile Reading reading;
  private volatile String status = "Connecting";

  Reading reading() {
    return reading;
  }

  String status() {
    return status;
  }

  private final Binder callback =
      new Binder() {
        @Override
        protected boolean onTransact(int code, Parcel data, Parcel reply, int flags)
            throws RemoteException {
          if (code == INTERFACE_TRANSACTION) {
            if (reply != null) reply.writeString(CALLBACK);
            return true;
          }
          if (code < 1 || code > 3) return super.onTransact(code, data, reply, flags);
          try {
            data.enforceInterface(CALLBACK);
            if (code == 1 && data.readInt() != 0) {
              long rpm = data.readLong(), power = data.readLong();
              data.readLong();
              data.readLong();
              int resistance = data.readInt();
              data.readInt();
              if (rpm < 0 || power < 0 || resistance < 0 || resistance > 100)
                throw new IllegalArgumentException("Invalid metrics");
              reading = new Reading(rpm, power, resistance);
              status = "Live";
            } else if (code == 2) {
              status = "Sensor error " + data.readLong();
              reading = null;
            }
            // The unused parcel tail and calibration notifications are deliberately ignored.
          } catch (RuntimeException e) {
            reading = null;
            status = "Unsupported sensor data";
            Log.w("BikeTube", status, e);
          }
          return true;
        }
      };
  private final ServiceConnection connection =
      new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
          work.post(
              () -> {
                if (closed) return;
                remote = binder;
                try {
                  if (!INTERFACE.equals(binder.getInterfaceDescriptor()))
                    throw new IllegalStateException("Unexpected interface");
                  registration(1);
                  status = "Waiting for readings";
                } catch (Exception e) {
                  reading = null;
                  status = "Sensor access unavailable";
                  Log.w("BikeTube", status, e);
                }
              });
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
          work.post(
              () -> {
                remote = null;
                reading = null;
                status = "Reconnecting";
              });
        }

        @Override
        public void onBindingDied(ComponentName name) {
          work.post(
              () -> {
                release();
                if (!closed) work.postDelayed(() -> bind(), 2000);
              });
        }

        @Override
        public void onNullBinding(ComponentName name) {
          status = "Unsupported sensor interface";
        }
      };

  SensorClient(Context c) {
    context = c.getApplicationContext();
    worker.start();
    work = new Handler(worker.getLooper());
    callback.attachInterface(null, CALLBACK);
  }

  void start() {
    work.post(() -> bind());
  }

  private void bind() {
    if (closed || bound) return;
    try {
      bound =
          context.bindService(
              new Intent(INTERFACE)
                  .setComponent(new ComponentName(PACKAGE, PACKAGE + ".AffernetService")),
              connection,
              Context.BIND_AUTO_CREATE);
      if (!bound) status = "Sensor service unavailable";
    } catch (RuntimeException e) {
      status = "Sensor access unavailable";
      Log.w("BikeTube", status, e);
    }
  }

  private void registration(int code) throws RemoteException {
    Parcel request = Parcel.obtain(), reply = Parcel.obtain();
    try {
      request.writeInterfaceToken(INTERFACE);
      request.writeStrongBinder(callback);
      request.writeString(context.getPackageName());
      if (!remote.transact(code, request, reply, 0))
        throw new RemoteException("Unsupported transaction");
      reply.readException();
    } finally {
      request.recycle();
      reply.recycle();
    }
  }

  private void release() {
    if (remote != null)
      try {
        registration(2);
      } catch (Exception e) {
        Log.d("BikeTube", "Sensor disconnected during cleanup");
      }
    if (bound) {
      try {
        context.unbindService(connection);
      } catch (IllegalArgumentException ignored) {
      }
      bound = false;
    }
    remote = null;
    reading = null;
  }

  @Override
  public void close() {
    work.post(
        () -> {
          closed = true;
          work.removeCallbacksAndMessages(null);
          release();
          worker.quitSafely();
        });
  }
}
