-- Written by Jarcode

local BLOCK = require("minesweeper_block")

GAME_WIDTH = 14;
GAME_HEIGHT = 8;
local BOMBS = 22;

local BLOCK_WIDTH = 22;
local BLOCK_HEIGHT = 22;
local H_MARGIN = 2;
local V_MARGIN = 2;

local blocks = {};

local buffer
INST = {}

-- function for blocks to obtain other blocks with
local function blockFunction(x, y)
    return blocks[x][y]
end

-- used to create game area
local function init()
    for i = 1,GAME_WIDTH do
        blocks[i] = {}
        for k = 1,GAME_HEIGHT do
            blocks[i][k] = BLOCK.Block.new(i, k, blockFunction)
        end
    end
end

-- used to iterate over a set of values that belong to a block
local function iterate(xoff, yoff, func)
    for x = xoff,BLOCK_WIDTH - 1 + xoff do
        for y = yoff,BLOCK_HEIGHT - 1 + yoff do
            func(x, y)
        end
    end
end

-- same as above, but used to outline stead
local function outline(xoff, yoff, func)
    for x = xoff,BLOCK_WIDTH - 1 + xoff do
        for y = yoff,BLOCK_HEIGHT - 1 + yoff do
            if (x == xoff or x == BLOCK_WIDTH - 1 + xoff
                    or y == yoff or y == BLOCK_HEIGHT - 1 + yoff) then
                func(x, y)
            end
        end
    end
end

-- function to randomly place bombs
local function placeBombs()
    for i = 1,BOMBS do
        local x = math.floor(math.random() * GAME_WIDTH) + 1
        local y = math.floor(math.random() * GAME_HEIGHT) + 1
        local blk = blocks[x][y]
        blk:setBomb(true)
    end
end

local function start()
    -- init game area
    init()
    -- place bombs
    placeBombs()
    -- allocate screen buffer
    buffer = screenBuffer(4)
    -- switch screen session
    switchSession(4)
    local running = true
    local first = true
    while (running) do

        -- grab interaction events, store them into array
        local actions = {}
        local index = 1;
        local at = buffer:pollCoords()
        while (at ~= nil) do
            actions[index] = at;
            index = index + 1
            at = buffer:pollCoords()
        end

        -- iterate
        for i = 1,#actions do
            local x = math.floor((actions[i]:x() - H_MARGIN) / BLOCK_WIDTH)
            local y = math.floor((actions[i]:y() - V_MARGIN) / BLOCK_HEIGHT)
            if (x >= 1 and x <= GAME_WIDTH and y >= 1 and y <= GAME_HEIGHT) then
                local blk = blocks[x][y]
                -- hit mine
                if (blk:onClick()) then
                    printc("&cYou lose!")
                    running = false
                    actions = {}
                    break;
                end
            end
        end

        -- only update if someone interacted
        if (#actions > 0 or first) then
            -- create frame
            local frame = screenFrame();
            -- background
            frame:fill(68)
            -- render cycle
            for i = 1,GAME_WIDTH do
                for k = 1,GAME_HEIGHT do
                    local blk = blocks[i][k]
                    local ax = (BLOCK_WIDTH * i) + H_MARGIN;
                    local ay = (BLOCK_HEIGHT * k) + V_MARGIN;
                    local s = blk:getState()
                    if (s == BLOCK.state("COVERED")) then
                        iterate(ax, ay, function(x, y)
                            frame:set(x, y, 91)
                        end)
                        outline(ax, ay, function(x, y)
                            frame:set(x, y, 87)
                        end)
                    elseif (s == BLOCK.state("REVEALED")) then
                        iterate(ax, ay, function(x, y)
                            frame:set(x, y, 90)
                        end)
                        outline(ax, ay, function(x, y)
                            frame:set(x, y, 88)
                        end)
                        local count = blk:countAdj();
                        if (count > 0) then
                            local adj = "&1" .. count
                            local len = frame:len(adj)
                            local xoff = math.round((GAME_WIDTH / 2) - (len / 2)) + 4
                            local yoff = math.round((GAME_HEIGHT / 2) + 4)
                            frame:write(ax + xoff, ay + yoff, adj)
                        end
                    end
                end
            end
            -- end render cycle

            -- update buffer
            buffer:update(frame:id())
        end

        -- win condition
        local check = true
        for i = 1,GAME_WIDTH do
            for k = 1,GAME_HEIGHT do
                local blk = blocks[i][k]
                local s = blk:getState()
                if (s == BLOCK.state("COVERED") and not blk:isBomb()) then
                    check = false
                end
            end
        end

        if (check) then
            printc("&aYou win!")
            running = false
        end

        first = false

        -- wait a bit
        sleep(65)
    end
    -- free buffer resources
    buffer:destroy()
    -- switch screen session
    switchSession(1)
end
INST.start = start;
INST.buffer = buffer;

-- main method
function main(args)
    -- trim string
    args = string.gsub(args, "%s$", "")
    -- convert string to number
    local num = tonumber(args)
    -- change number of bombs if converted
    if (num ~= null) then
        BOMBS = num
    end
    -- start the game!
    start();
end

return INST;
