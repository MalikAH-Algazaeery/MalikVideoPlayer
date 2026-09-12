use mcrx_core::{Context, SubscriptionConfig, SubscriptionId};
use std::net::Ipv4Addr;
use std::sync::Mutex; // Needed for thread safety

// We must "derive" Object so UniFFI knows this is a class for Kotlin
// We wrap the internal state in a Mutex so it's safe to move to Android
#[derive(uniffi::Object)]
pub struct MulticastReceiver {
    inner: Mutex<ReceiverInner>,
}

struct ReceiverInner {
    ctx: Context,
    _id: SubscriptionId,
}

#[uniffi::export]
impl MulticastReceiver {
    // This is the constructor Kotlin calls to create the object
    #[uniffi::constructor]
    pub fn new(group: String, port: u16, my_ip: String) -> Self {
        let mut ctx = Context::new();
        let addr: Ipv4Addr = group.parse().expect("Invalid Group IP");
        let local_iface: Ipv4Addr = my_ip.parse().expect("Invalid Phone IP");
        
        let mut config = SubscriptionConfig::asm(addr, port);
        config.interface = Some(local_iface.into());
        
        let id = ctx.add_subscription(config).unwrap();
        ctx.join_subscription(id).unwrap();
        
        Self { 
            inner: Mutex::new(ReceiverInner { ctx, _id: id }) 
        }
    }

    // Notice we use &self now because it's an object method
    pub fn read_packets_batch(&self) -> Vec<u8> {
        let mut inner = self.inner.lock().unwrap();
        let mut batch_buffer = Vec::new();

        // Read all waiting packets until the queue is empty
        // or we hit a reasonable limit (e.g., 64KB)
        while let Ok(Some(packet)) = inner.ctx.try_recv_any() {
            batch_buffer.extend_from_slice(packet.payload());
            
            if batch_buffer.len() > 65536 { 
                break; 
            }
        }
        
        batch_buffer
    }
}

uniffi::setup_scaffolding!();