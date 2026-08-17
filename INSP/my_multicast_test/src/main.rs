// We are using the "Core Flow" from your mcrx-core instructions
use mcrx_core::{Context, SubscriptionConfig};
use std::net::Ipv4Addr;

fn main() -> Result<(), Box<dyn std::error::Error>> {
    // 1. Create the Context (The "Manager" of the library)
    let mut ctx = Context::new();

    // 2. Set the address (239.1.2.3) and port (5000)
    let group = Ipv4Addr::new(239, 1, 2, 3);
    let port = 5000;

    // 3. Create the configuration
    let config = SubscriptionConfig::asm(group, port);

    // 4. Add the subscription and get an ID
    let id = ctx.add_subscription(config)?;

    // 5. Join (This tells your Fritzbox to start sending data to your PC)
    ctx.join_subscription(id)?;

    println!("Listening for multicast on 239.1.2.3:5000...");

    // 6. Loop forever to check for packets
    loop {
        // This is the "try_recv_any" from your instructions
        if let Some(packet) = ctx.try_recv_any()? {
            println!("Received {} bytes!", packet.payload_len());
        }
        // Small sleep so we don't use 100% of your CPU
        std::thread::sleep(std::time::Duration::from_millis(10));
    }
}