-- Written by Jarcode

require("list")

Server = {
    -- strings
    channel,

    -- channel
    ch,

    -- message handler
    messageHandler = nil,
}
Server.__index = Server

Target = {
    computer, channel,
    send = send
}
Target.__index = Target

-- creates a target to send messages to
function Target.new(hostname, channel)
    local comp = findComputer(hostname)
    if comp == nil then return end
    local self = setmetatable({}, Target)
    self.computer = comp
    self.channel = channel
    return self
end

-- sends a message to this target
function Target:send(message)
    self.computer:message(self.channel, message)
end

-- creates a new server
function Server.new(channel)
    local self = setmetatable({}, Server)
    self.channel = channel
    self.ch = registerChannel(channel)
    self.clients = List.new()
    return self
end

-- sends a message to a computer, using the channel of this server
function Server:send(hostname, message)
    local comp = findComputer(hostname)
    message = message:gsub("\0", "?")
    if comp == nil then return end
    comp.message(self.channel, message)
end

-- handles raw input (internal function)
function Server:handle(tbl)

    -- message codes; 1 is a standard message
    -- messages are separated by null characters

    if (tbl[1] == "1") then

        -- format (hostname, message)

        -- call handle if registered
        if (this.messageHandler ~= nil) then this.messageHandler(tbl[2], tbl[3]) end

        -- return response
        return {tbl[2], tbl[3]}
    end
end

-- polls the channel for messages
function Server:poll()
    local msg
    repeat
        msg = self.ch:poll()
        local spl = split(msg, "\0")
        self:handle(spl)
    until msg == nil
end

-- sets the handler for messages, it should accept two strings: (channel, message)
function Server:setHandler(func)
    this.messageHandler = func
end
-- functions
Server.send = send
Server.poll = poll
Server.addClient = addClient