Change Log
==========

Version 2.0.0 *(2020-02-13)*
----------------------------
Kotlin suspend function support!


Version 1.2.6 *(2020-01-25)*
----------------------------
Proxy equals now match with the id of the stub process it wraps
Better Proxy toString 

Version 1.2.4 *(2019-06-14)*
----------------------------
Propagate exception from @Oneway 

Version 1.2.0 *(2019-02-11)*
----------------------------
Prevent name collisions


Version 1.1.9 *(2018-01-12)*
----------------------------

Support for binder[]
cleanup methods for proxy/stub


Version 1.1.8 *(2018-08-28)*
----------------------------

Proxy returned of same service instance will have the same identity


Version 1.1.5 *(2018-02-02)*
----------------------------

All Proxy now implement RemoteProxy interface that exposes methods to listen for binder death


Version 1.1.2 *(2017-10-7)*
----------------------------

Clients get the same exception that is thrown by the remote implementation.


Version 1.1.0 *(2017-10-4)*
----------------------------

Supports extending other interfaces

Version 1.0.4 *(2017-09-30)*
----------------------------

Exposes methods in proxy to listen for remote process death


Version 1.0.2 *(2017-09-21)*
----------------------------

Support for Parceler (@Parcel)


Version 1.0.1 *(2017-09-16)*
----------------------------

Support for @Oneway to mark a method as asynchronous


Version 1.0.0 *(2017-08-23)*
----------------------------

Initial release.
