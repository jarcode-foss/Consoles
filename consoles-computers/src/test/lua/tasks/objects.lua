
return function()
    local foo = newFooObject();
    log("object string value: " .. tostring(foo))
    if foo.foo == nil or foo.bar == nil then
        error("could not find 'foo.foo' or/and 'foo.bar'")
    end
    log("function values: " .. tostring(foo.foo) .. ", " .. tostring(foo.bar))
    log("foo:foo() = " .. foo:foo())
    log("foo:bar(2, 10) = " .. foo:bar(2, 10))
    log("releasing object")
    if foo.release == nil then
        error("release function not available")
    end
    foo:release()
    if pcall(function() foo:foo() end) then
        error("released object did not error as expected")
    else
        log("error (expectedly) raised when object accessed after being released")
    end
end
