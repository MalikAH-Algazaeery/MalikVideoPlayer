// Note i wrote this in the begining just to test rust on my computer (not needed anymore for the android project)
// I am using the "Core Flow" from mcrx-core instructions
use mcrx_core::{Context, SubscriptionConfig};
use std::net::Ipv4Addr;

fn main() -> Result<(), Box<dyn std::error::Error>> {
    // Create the Context (The "Manager" of the library)
    let mut ctx = Context::new();

    // Set the address (239.1.2.3) and port (5000)
    let group = Ipv4Addr::new(239, 1, 2, 3);
    let port = 5000;

    // Create the configuration
    let config = SubscriptionConfig::asm(group, port);

    // Add the subscription and get an ID
    let id = ctx.add_subscription(config)?;

    // Join (This tells the router to start sending data)
    ctx.join_subscription(id)?;

    println!("Listening for multicast on 239.1.2.3:5000...");

    // Loop forever to check for packets
    loop {
        // This is the "try_recv_any" from the rust API instructions
        if let Some(packet) = ctx.try_recv_any()? {
            println!("Received {} bytes!", packet.payload_len());
        }
        // Small sleep so we don't use 100% of CPU
        std::thread::sleep(std::time::Duration::from_millis(10));
    }
}